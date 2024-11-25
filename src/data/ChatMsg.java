package data;

import javax.swing.*;

// 채팅 메시지 & 게임 시작 전 메시지 클래스
public class ChatMsg extends BaseMsg {
    private ImageIcon image;   // 이미지 아이콘
    private final byte[] imageBytes; // 이미지 데이터를 바이트 배열로 저장
    private final String fileName;   // 파일 이름
    private final long fileSize;     // 파일 크기
    private final String textMessage; // 텍스트 메시지

    private ChatMsg(Builder builder) {
        super(builder);
        this.image = builder.image;
        this.imageBytes = builder.imageBytes;
        this.fileName = builder.fileName;
        this.fileSize = builder.fileSize;
        this.textMessage = builder.textMessage;
    }

    // Getters
    public ImageIcon getImage() {
        return image;
    }

    // 이미지 데이터를 바이트 배열로 가져옴
    public byte[] getImageBytes() {
        return imageBytes;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public void setImage(ImageIcon image) {
        this.image = image;
    }

    // Builder 클래스
    public static class Builder extends BaseMsg.Builder<Builder> {
        private ImageIcon image;
        private byte[] imageBytes;
        private String fileName;
        private long fileSize;
        private String textMessage; // 텍스트 메시지

        public Builder(String code) {
            super(code);
        }

        public Builder image(ImageIcon image) {
            this.image = image;
            return this;
        }

        public Builder imageBytes(byte[] imageBytes) {
            this.imageBytes = imageBytes;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder textMessage(String textMessage) {
            this.textMessage = textMessage;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public ChatMsg build() {
            return new ChatMsg(this);
        }
    }


}