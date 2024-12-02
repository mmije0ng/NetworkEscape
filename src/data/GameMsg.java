package data;

import java.util.List;

// 게임 상태 데이터 전송 클래스
public class GameMsg extends BaseMsg {
    private final int x; // X 좌표
    private final int y; // Y 좌표
    private final List<int[]> blocks; // block 위에 아이템 생성을 위함(x,y,width,height)
    private final List<int[]> items; //Server가 생성한 아이템들(x,y,type)

    private final int[] gotItem; //유저가 획득한 아이템 1개의 정보
    private final int point;    //포인트

    //Todo: Item X,Y,Type 추가
    private GameMsg(Builder builder) {
        super(builder);
        this.x = builder.x;
        this.y = builder.y;
        this.blocks = builder.blocks;
        this.items = builder.items;
        this.point = builder.point;
        this.gotItem = builder.gotItem;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

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


    // Builder 클래스
    public static class Builder extends BaseMsg.Builder<Builder> {
        private int x;
        private int y;
        private List<int[]> blocks;
        private List<int[]> items;
        private int point;
        private int[] gotItem;
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
