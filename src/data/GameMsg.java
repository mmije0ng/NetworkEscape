package data;

// 게임 상태 데이터 전송 클래스
public class GameMsg extends BaseMsg {
    private final int x; // X 좌표
    private final int y; // Y 좌표

    private GameMsg(Builder builder) {
        super(builder);
        this.x = builder.x;
        this.y = builder.y;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // Builder 클래스
    public static class Builder extends BaseMsg.Builder<Builder> {
        private int x;
        private int y;

        public Builder(String code) {
            super(code);
        }

        public Builder x(int x) {
            this.x = x;
            return this;
        }

        public Builder y(int y) {
            this.y = y;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public GameMsg build() {
            return new GameMsg(this);
        }
    }
}
