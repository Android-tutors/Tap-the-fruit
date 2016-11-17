package abc.games.tapthefruit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class ResourceFactory {
    // список текстур
    Texture background;

    // фрукты, вопрос, рамки
    TextureRegion[] fruitSprites;
    Animation question, wrongBorder, goodBorder;

    Music music;

    Sound goodSound, wrongSound;

    BitmapFont font;

    public ResourceFactory(int fruits_count) {
        background = new Texture("background.jpg");
        Texture _fruits = new Texture("fruits.png");

        // ФРУКТЫ
        //
        fruitSprites = new TextureRegion[fruits_count];

        for (int i = 0; i < fruits_count; i++)
            fruitSprites[i] = new TextureRegion(_fruits, i * 70, 0, 70, 70);

        // ВОПРОС и РАМКИ
        //
        TextureRegion[] tr = new TextureRegion[8];
        for (int i = 0; i < 8; i++)
            tr[i] = new TextureRegion(_fruits, i * 70, 70, 70, 70);

        question = new Animation(0.05f, tr[0], tr[1], tr[2], tr[3]);
        question.setPlayMode(Animation.PlayMode.LOOP);

        wrongBorder = new Animation(.1f, tr[4], tr[5]);
        wrongBorder.setPlayMode(Animation.PlayMode.LOOP);

        goodBorder = new Animation(.1f, tr[6], tr[7]);
        goodBorder.setPlayMode(Animation.PlayMode.LOOP);

        // загружаем музыку
        music = Gdx.audio.newMusic(Gdx.files.internal("back.mp3"));
        music.setVolume(.5f);
        music.setLooping(true);
        music.play();

        goodSound = Gdx.audio.newSound(Gdx.files.internal("claps.mp3"));
        wrongSound = Gdx.audio.newSound(Gdx.files.internal("boo.mp3"));

        // загрузка шрифта
        font = new BitmapFont(Gdx.files.internal("Candara.fnt"));
    }
}
