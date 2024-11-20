package data;

import java.io.Serializable;

// 메시지의 기본 클래스 (추상 클래스)
public abstract class BaseMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String code;       // 메시지 유형
    private final String roomName;       // 게임방 이름
    private final Integer gameMode;  // 게임 모드 (1대1, 2대2)
    private final String nickname;   // 닉네임
    private final Integer team;      // 팀1, 팀2
    private final String message;    // 메시지
    private final String character;  // 캐릭터 이름

    // protected 생성자 (빌더에서만 호출 가능)
    protected BaseMsg(Builder<?> builder) {
        this.code = builder.code;
        this.roomName = builder.room;
        this.gameMode = builder.gameMode;
        this.nickname = builder.nickname;
        this.team = builder.team;
        this.message = builder.message;
        this.character = builder.character;
    }

    // Getters
    public String getCode() {
        return code;
    }

    public String getRoomName() {
        return roomName;
    }

    public Integer getGameMode() {
        return gameMode;
    }

    public String getNickname() {
        return nickname;
    }

    public Integer getTeam() {
        return team;
    }

    public String getMessage() {
        return message;
    }

    public String getCharacter() {
        return character;
    }

    // 빌더 클래스 (제네릭)
    public abstract static class Builder<T extends Builder<T>> {
        private final String code;
        private String room;
        private Integer gameMode;
        private String nickname;
        private Integer team;
        private String message;
        private String character;

        public Builder(String code) {
            this.code = code;
        }

        public T room(String room) {
            this.room = room;
            return self();
        }

        public T gameMode(Integer gameMode) {
            this.gameMode = gameMode;
            return self();
        }

        public T nickname(String nickname) {
            this.nickname = nickname;
            return self();
        }

        public T team(Integer team) {
            this.team = team;
            return self();
        }

        public T message(String message) {
            this.message = message;
            return self();
        }

        public T character(String character) {
            this.character = character;
            return self();
        }

        protected abstract T self();

        public abstract BaseMsg build();
    }
}