// 파일명: migrate-images.ts

import db from './src/db';
import sharp from 'sharp';
import fs from 'fs';
import path from 'path';

const UPLOAD_DIR = path.join(__dirname, 'uploads');

async function migrateImages() {
    console.log("🚀 기존 이미지 일괄 압축 작업을 시작합니다...");

    const client = await db.getClient();

    try {
        // DB 쿼리는 트랜잭션 내부에서 처리 (파일 I/O는 트랜잭션 밖에서 처리하는 것이 좋지만, 현재는 통일성을 위해 유지)
        await client.query('BEGIN');

        // 상세 이미지 처리
        const resImages = await client.query('SELECT image_id, image_url FROM post_image');
        const images = resImages.rows;
        console.log(`👉 총 ${images.length}개의 상세 이미지를 발견했습니다.`);

        // 병렬로 처리하는 것이 좋지만, 안전을 위해 순차 처리 유지
        for(const img of images) {
            await processOneImage(client, 'post_image', 'image_id', img.image_id, img.image_url);
        }

        // 썸네일 이미지 처리
        const resPosts = await client.query('SELECT post_id, thumbnail_url FROM post WHERE thumbnail_url IS NOT NULL');
        const posts = resPosts.rows;
        console.log(`👉 총 ${posts.length}개의 썸네일 이미지를 발견했습니다.`);

        // 안전을 위해 순차 처리 유지
        for(const post of posts) {
            await processOneImage(client, 'post', 'post_id', post.post_id, post.thumbnail_url);
        }

        await client.query('COMMIT');
        console.log("🎉 모든 이미지 마이그레이션이 DB에 커밋되었습니다."); // 🔥 커밋 성공 메시지 추가
    } catch(error) {
        await client.query('ROLLBACK');
        console.error("🚨 치명적인 에러 발생! 전체 트랜잭션 롤백:", error); // 🔥 에러 메시지 상세화
    } finally {
        client.release();
        process.exit();
    }
}

// migrate-images.ts 파일 내, processOneImage 함수
async function processOneImage(client: any, tableName: string, idColumn: string, idValue: number, dbPath: string) {
    if (!dbPath) return;

    if (dbPath.includes('resized-')) {
        return;
    }

    const filename = path.basename(dbPath);
    const originalFilePath = path.join(UPLOAD_DIR, filename);

    // 🔥 여기서 파일 존재 여부 체크를 분리합니다.
    let fileExists = fs.existsSync(originalFilePath);

    const resizedFilename = `resized-${filename}`;
    const resizedFilePath = path.join(UPLOAD_DIR, resizedFilename);
    const newDbPath = `uploads/${resizedFilename}`.replace(/\\/g, "/");

    try {
        if (fileExists) {
            // 1. 파일이 존재하는 경우에만 압축/삭제 수행
            await sharp(originalFilePath)
                .resize({ width: 1024, withoutEnlargement: true })
                .withMetadata()
                .jpeg({ quality: 80 })
                .toFile(resizedFilePath);

            fs.unlinkSync(originalFilePath); // 원본 삭제
            
            console.log(`✅ 변환 완료: ${filename} -> ${resizedFilename}`);
        } else {
            // 2. 파일이 없는 경우
            // 경로만 업데이트하고 압축/삭제는 건너뜁니다.
            console.warn(`⚠️ 파일 없음 (경로만 업데이트): ${originalFilePath}`); 
        }

        // 🔥 경로 업데이트는 파일 존재 여부와 관계없이 무조건 수행합니다.
        const updateQuery = `UPDATE ${tableName} SET ${tableName === 'post' ? 'thumbnail_url' : 'image_url'} = $1 WHERE ${idColumn} = $2`;
        await client.query(updateQuery, [newDbPath, idValue]);

    } catch (err) {
        // DB 업데이트 또는 파일 처리 중 에러 발생 시, 로그를 남기고 다음 이미지로 넘어갑니다.
        // 이 에러는 바깥 트랜잭션에는 영향을 주지 않아야 합니다.
        console.error(`❌ 처리 실패 (${filename}): 다음 이미지 처리 계속...`, err);
    }
}

migrateImages();