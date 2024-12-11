package data;

import java.util.List;
import java.util.Map;

// 게임 상태 데이터 전송 클래스
public class GameMsg extends BaseMsg {
    private final int x; // X 좌표
    private final int y; // Y 좌표
    private final int level; // 현재 레벨
    private final int currentDoorIndex; // 현재 열리는 문 인덱스
    private final List<int[]> blocks; // block 위에 아이템 생성을 위함(x,y,width,height)
    private final List<int[]> items; //Server가 생성한 아이템들(x,y,type)
    private final int[] gotItem; //유저가 획득한 아이템 1개의 정보
    private final int point;    //포인트
    private final Integer winTeam; // 이긴 팀
    private final Integer winPoint; // 이긴 팀 점수
    private final Map<String, String> winners; // 게임 승자 맵 (닉네임, 캐릭터)

    //Todo: Item X,Y,Type 추가
    private GameMsg(Builder builder) {
        super(builder);
        this.x = builder.x;
        this.y = builder.y;
        this.level = builder.level;
        this.currentDoorIndex = builder.currentDoorIndex;
        this.blocks = builder.blocks;
        this.items = builder.items;
        this.point = builder.point;
        this.gotItem = builder.gotItem;
        this.winTeam = builder.winTeam;
        this.winPoint = builder.winPoint;
        this.winners = builder.winners;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getLevel() {
        return level;
    }
    public int getCurrentDoorIndex() {return currentDoorIndex;}
    public List<int[]> getBlocks(){
        return blocks;
    }

    public List<int[]> getItems(){
        return items;
    }

    public int getPoint(){
        return point;
    }

    public int[] getGotItem(){
        return gotItem;
    }
    public Integer getWinTeam() {return winTeam;}
    public Integer getWinPoint() {return  winPoint;}
            
    public Map<String, String> getWinners() {return winners;}


    // Builder 클래스
    public static class Builder extends BaseMsg.Builder<Builder> {
        private int x;
        private int y;
        private int level;
        private int currentDoorIndex;
        private List<int[]> blocks;
        private List<int[]> items;
        private int point;
        private int[] gotItem;
        private Integer winPoint;
        private Integer winTeam;
        private Map<String, String> winners;

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

        public Builder level(int level) {
            this.level = level;
            return this;
        }

        public Builder currentDoorIndex(int currentDoorIndex) {
            this.currentDoorIndex = currentDoorIndex;
            return this;
        }

        public Builder blocks(List<int[]> blocks){
            this.blocks = blocks;
            return this;
        }

        public Builder items(List<int[]> items){
            this.items = items;
            return this;
        }
        public Builder point(int point){
            this.point =point;
            return this;
        }
        public Builder gotItem(int[] gotItem){
            this.gotItem =gotItem;
            return this;
        }

        public Builder winTeam(Integer winTeam){
            this.winTeam =winTeam;
            return this;
        }

        public Builder winPoint(Integer winPoint){
            this.winPoint =winPoint;
            return this;
        }

        public Builder winners(Map<String, String> winners) {
            this.winners = winners;
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
