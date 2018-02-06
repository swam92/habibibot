DROP TABLE IF EXISTS content;

CREATE TABLE content (
       send_time timestamp,
       sender_id varchar(255),
       recipient_id varchar(255),
       mid varchar(255),
       seq integer,
       image_fb_url varchar(255),
       video_fb_url varchar(255),
       s3_id varchar(255),
       content_hash varchar(255)
)

