INSERT INTO items (id, has_image, has_video, unread, date_resolved, user_id, mime_type, resolved_url, title, url)
VALUES
    (1, true, true, true, '2020-01-01', 1, 'text', 'https://practicum.yandex.ru/java-developer/',
     'Курс «Java-разработчик» с нуля: онлайн-обучение Java-программированию для начинающих — Яндекс Практикум', 'https://bit.ly/3vRVvO0'),
    (2, false, true, false, '2022-01-01', 1, 'video', 'https://some-resolved-url-video.com', 'some title', 'https://some-video-url'),
    (3, true, false, true, '2023-01-01', 1, 'image', 'https://some-resolved-url-image.com', 'some title', 'https://some-image-url');