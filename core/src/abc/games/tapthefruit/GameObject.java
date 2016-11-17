package abc.games.tapthefruit;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class GameObject extends Sprite {

    public boolean inBounds(float x, float y) {
        float left = this.getX(), top = this.getY(),
                right = left + this.getWidth(), bottom = top + this.getHeight();
        return (x > left && y > top && x < right && y < bottom);
    }
}
