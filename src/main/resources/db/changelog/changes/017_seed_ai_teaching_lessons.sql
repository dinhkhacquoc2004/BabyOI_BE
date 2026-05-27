CREATE TABLE IF NOT EXISTS ai_teaching_lesson (
    id BIGSERIAL PRIMARY KEY,
    lesson_name VARCHAR(255) NOT NULL,
    description TEXT,
    reasonable_age_from_month INTEGER,
    reasonable_age_to_month INTEGER,
    recommended_duration_days INTEGER,
    lesson_note TEXT,
    video_url TEXT,
    suggested_by_organization VARCHAR(255),
    source_url TEXT,
    source_organization VARCHAR(255),
    icon_url TEXT,
    sort_order INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    status BIGINT
);

ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS lesson_name VARCHAR(255);
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS reasonable_age_from_month INTEGER;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS reasonable_age_to_month INTEGER;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS recommended_duration_days INTEGER;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS lesson_note TEXT;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS video_url TEXT;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS suggested_by_organization VARCHAR(255);
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS source_url TEXT;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS source_organization VARCHAR(255);
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS icon_url TEXT;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS sort_order INTEGER;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE ai_teaching_lesson ADD COLUMN IF NOT EXISTS status BIGINT;

INSERT INTO ai_teaching_lesson (
    lesson_name,
    description,
    reasonable_age_from_month,
    reasonable_age_to_month,
    recommended_duration_days,
    lesson_note,
    video_url,
    suggested_by_organization,
    source_url,
    source_organization,
    icon_url,
    sort_order,
    created_at,
    updated_at,
    created_by,
    updated_by,
    status
)
SELECT
    seed.lesson_name,
    seed.description,
    seed.reasonable_age_from_month,
    seed.reasonable_age_to_month,
    seed.recommended_duration_days,
    seed.lesson_note,
    seed.video_url,
    seed.suggested_by_organization,
    seed.source_url,
    seed.source_organization,
    seed.icon_url,
    seed.sort_order,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'SYSTEM',
    'SYSTEM',
    2
FROM (
    VALUES
        ('Tummy Time (Nằm sấp)', 'Đặt bé nằm sấp khi thức để phát triển cơ cổ, vai, lưng, chuẩn bị cho việc lẫy và bò.', 0, 3, 90, 'Chỉ thực hiện khi bé thức và có sự giám sát 100%. Không tập khi vừa ăn no.', 'https://youtu.be/W7mH_N6uS6U', 'Pathways Org', 'https://www.cdc.gov/act-early/milestones/milestones-2mo.html', 'CDC Hoa Kỳ', NULL, 1),
        ('Serve & Return (Tương tác phản hồi)', 'Phản hồi lại mỗi khi bé phát ra âm thanh, cười hoặc nhìn cha mẹ để kích thích vùng não giao tiếp.', 0, 3, 90, 'Hãy đợi bé phát tín hiệu trước, sau đó mới phản hồi để tạo thói quen hội thoại nhịp nhàng.', 'https://youtu.be/m_5u8-QSh6A', 'ĐH Harvard', 'https://www.unicef.org/parenting/child-development/your-babys-developmental-milestones', 'UNICEF', NULL, 2),
        ('Đọc sách tranh tương phản', 'Sử dụng sách tranh đen - trắng, hình khối rõ nét ở khoảng cách 20-30cm để rèn luyện sự tập trung của thị giác.', 3, 6, 90, 'Tránh các loại sách có quá nhiều chi tiết gây nhiễu, ưu tiên sách vải an toàn vì bé có xu hướng gặm đồ vật.', 'https://youtu.be/u88v9U7e9V4', 'BookTrust', 'https://www.cdc.gov/act-early/milestones/milestones-4mo.html', 'CDC Hoa Kỳ', NULL, 3),
        ('Tập lẫy & Cầm nắm', 'Khuyến khích lật bằng cách đặt đồ chơi một bên hông. Đưa đồ chơi xúc xắc để bé tập cầm, lắc tạo âm thanh.', 3, 6, 90, 'Đảm bảo không gian sàn bằng phẳng, không để vật sắc nhọn hoặc chăn gối lỏng lẻo dễ gây ngạt xung quanh.', 'https://youtu.be/3wEwB1fUv-Q', 'Pathways Org', 'https://www.who.int/news-room/fact-sheets/detail/physical-activity', 'WHO', NULL, 4),
        ('Trò chơi Ú òa', 'Giấu đồ chơi dưới khăn hoặc che mặt rồi mở ra để dạy bé hiểu đồ vật không biến mất vĩnh viễn.', 6, 12, 180, 'Tăng dần độ khó bằng cách giấu một phần đồ chơi để bé tự dùng tay kéo khăn ra tìm kiếm.', 'https://youtu.be/2_6e_Yc70h0', 'UNICEF', 'https://www.cdc.gov/act-early/milestones/milestones-6mo.html', 'CDC Hoa Kỳ', NULL, 5),
        ('Tập bốc nhón', 'Cho bé nhặt các mẩu thức ăn mềm, nhỏ bằng ngón trỏ và ngón cái để rèn vận động tinh.', 6, 12, 180, 'Thức ăn phải đủ mềm để bóp nát bằng hai ngón tay. Tuyệt đối tránh các loại hạt tròn, cứng vì nguy cơ hóc dị vật.', 'https://youtu.be/vA0H_3AymY0', 'Pathways Org', 'https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding', 'WHO', NULL, 6),
        ('Tập bò & Đứng bám', 'Đặt đồ chơi ngoài tầm với để kích thích trườn bò. Thiết kế các khu vực bàn ghế vững chắc để bé tự vịn tay đứng lên.', 6, 12, 180, 'Phải bọc các góc nhọn của bàn ghế, lắp chặn cửa cầu thang trước khi bé bước vào giai đoạn này.', 'https://youtu.be/g_N_9zZg4_M', 'Pathways Org', 'https://www.cdc.gov/act-early/milestones/milestones-9mo.html', 'CDC Hoa Kỳ', NULL, 7),
        ('Chỉ tay & Gọi tên', 'Khuyến khích bé dùng ngón trỏ chỉ vào thứ mình muốn và cha mẹ gọi tên chính xác vật thể đó để phát triển vốn từ.', 12, 18, 180, 'Không nói ngọng, nói nhại theo giọng của bé. Hãy dùng từ ngữ chuẩn, rõ ràng và ngắn gọn.', 'https://youtu.be/S470q8u_D_g', 'First Words Project', 'https://www.cdc.gov/act-early/milestones/milestones-12mo.html', 'CDC Hoa Kỳ', NULL, 8),
        ('Xếp chồng khối gỗ', 'Hướng dẫn bé đặt 2 - 3 khối gỗ lên nhau mà không làm đổ để rèn luyện tư duy không gian và độ khéo của tay.', 12, 18, 180, 'Khen ngợi nỗ lực của bé ngay cả khi khối gỗ bị đổ, giúp trẻ xây dựng sự kiên trì.', 'https://youtu.be/p6pM6mFOf2w', 'LEGO Foundation', 'https://www.unicef.org/parenting/child-development/activities-for-babies-1-2-years-old', 'UNICEF', NULL, 9),
        ('Nói câu đôi & Chỉ bộ phận', 'Dạy bé ghép các từ đơn thành cụm từ (Ví dụ: Mẹ bế, Bóng đỏ) và chỉ đúng bộ phận trên cơ thể khi được hỏi.', 18, 24, 180, 'Nếu bé nói sai, không chê bai mà hãy lặp lại câu đúng.', 'https://youtu.be/im88zT_7Y_0', 'NHS (Y tế Anh)', 'https://www.cdc.gov/act-early/milestones/milestones-18mo.html', 'CDC Hoa Kỳ', NULL, 10),
        ('Phân loại hình khối/Màu sắc', 'Trò chơi thả khối vào lỗ tương ứng hoặc gom nhóm các món đồ có cùng màu sắc.', 18, 24, 180, 'Bắt đầu với 2 màu tương phản hoàn toàn trước, khi bé thuần thục mới tăng lên 3-4 màu hoặc phân loại theo hình dáng.', 'https://youtu.be/JyvG64Gid2Q', 'Rocking Dan', 'https://www.cdc.gov/act-early/milestones/milestones-24mo.html', 'CDC Hoa Kỳ', NULL, 11),
        ('Trò chơi bắt chước', 'Cho bé giả vờ nghe điện thoại, cho búp bê ăn, hoặc quét nhà bằng chổi đồ chơi để phát triển tư duy trừu tượng.', 18, 24, 180, 'Khuyến khích sử dụng các vật dụng an toàn trong nhà để tăng tính sáng tạo thay vì mua quá nhiều đồ chơi nhựa mô phỏng.', 'https://youtu.be/6S8Zf6MubE0', 'ĐH Harvard', 'https://www.who.int/news-room/fact-sheets/detail/improving-early-childhood-development', 'WHO', NULL, 12),
        ('Nhận biết cữ bú & Giao tiếp', 'Nhận biết dấu hiệu đói và no. Tương tác bằng ánh mắt với bé khi đang cho bú.', 0, 6, 180, 'Không ép bú theo giờ cứng nhắc, hãy cho bú theo nhu cầu. Mẹ cần giữ tinh thần thoải mái.', 'https://youtu.be/k6G3d4YVvN8', 'Global Health Media', 'https://www.who.int/health-topics/breastfeeding', 'WHO', NULL, 13),
        ('Bắt đầu ăn dặm & Giới thiệu thìa', 'Cho bé làm quen với thức ăn nhuyễn/mềm. Dạy bé há miệng khi thìa đến gần và ngậm miệng gạt thức ăn.', 6, 8, 60, 'Áp dụng nguyên tắc thử 3 ngày với 1 loại thực phẩm mới để theo dõi phản ứng dị ứng.', 'https://youtu.be/xJq3i3Z46V4', 'UNICEF', 'https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding', 'WHO', NULL, 14),
        ('Tập uống nước bằng cốc mở', 'Sử dụng cốc nhỏ (không nắp, không vòi) có chứa một ít nước để bé tự cầm bằng 2 tay và nhấp môi uống.', 6, 12, 180, 'Nên dùng cốc silicone nhỏ. Bé sẽ làm đổ nước nhiều lần lúc đầu, hãy kiên nhẫn và tập trong bữa ăn.', 'https://youtu.be/Fk_E3_7U-k8', 'Solid Starts', 'https://www.cdc.gov/nutrition/infantandtoddlernutrition/foods-and-drinks/index.html', 'CDC Hoa Kỳ', NULL, 15),
        ('Tập dùng thìa/muỗng độc lập', 'Cung cấp thìa ngắn, tay cầm to. Khuyến khích bé tự xúc đồ ăn đặc đưa vào miệng.', 12, 18, 180, 'Chấp nhận việc bé bôi bẩn. Đây là bước cực kỳ quan trọng để rèn luyện sự phối hợp tay - mắt.', 'https://youtu.be/5b2vX28z9Fk', 'Pathways Org', 'https://www.cdc.gov/act-early/milestones/milestones-18mo.html', 'CDC Hoa Kỳ', NULL, 16),
        ('Cai sữa bình & Ăn cùng gia đình', 'Giảm dần cữ sữa bình/mẹ, thay bằng sữa tươi (sau 1 tuổi) uống bằng cốc. Tăng cường 3 bữa chính, 2 bữa phụ.', 12, 24, 360, 'Cố gắng bỏ hoàn toàn bình sữa trước 18 tháng để bảo vệ răng. Không cai sữa đột ngột gây khủng hoảng tâm lý.', 'https://youtu.be/rV0XpL5zVwU', 'NHS (Y tế Anh)', 'https://www.nhs.uk/conditions/baby/weaning-and-feeding/drinks-and-cups-for-babies-and-young-children/', 'NHS (Y tế Anh)', NULL, 17)
) AS seed (
    lesson_name,
    description,
    reasonable_age_from_month,
    reasonable_age_to_month,
    recommended_duration_days,
    lesson_note,
    video_url,
    suggested_by_organization,
    source_url,
    source_organization,
    icon_url,
    sort_order
)
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_teaching_lesson existing
    WHERE existing.lesson_name = seed.lesson_name
);

