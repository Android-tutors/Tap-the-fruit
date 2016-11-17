package abc.games.tapthefruit;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.Random;

public class TapTheFruit extends Game {

    final boolean DEBUG = false;

    final Random random = new Random();

    // состояния игры
    final int STATE_START = 0;      // самое начало
    final int STATE_COME = 1;       // фрукты прибегают
    final int STATE_STAY = 2;       // фрукты запоминаются
    final int STATE_QUESTION = 3;   // ожидание ответа
    final int STATE_WRONG = 4;      // ошибка
    final int STATE_GOOD = 5;       // верно
    final int STATE_AWAY = 6;       // фрукты убегают

    // время в разных состояниях
    final int[] TIME_IN_STATE = new int[]{
            0,  // STATE_START
            1,  // STATE_COME
            1,  // STATE_STAY
            5,  // STATE_QUESTION
            2,  // STATE_WRONG
            2,  // STATE_GOOD
            1   // STATE_AWAY
    };

    // сколько разных фруктов
    final int FRUITS_COUNT = 8;
    // сколько фруктов для выбора
    final int SELECT_FRUITS_COUNT = 3;

    // ширина украна фиксированная
    final float SCREEN_WIDTH = 190f;
    // высота экрана вычисляется
    float SCREEN_HEIGHT;
    // отношение реальной ширины и высоты экрана к установленному размеру
    float SCREEN_KOEF;

    // загрузчик ресурсов
    ResourceFactory RF;

    SpriteBatch batch;
    ShapeRenderer shapeRenderer;
    OrthographicCamera camera, uiCamera;

    // состояние и время
    int gameState;
    float stateDelta;

    // какие фрукты загаданы
    float[] showFruitsX = new float[4];
    GameObject[] showFruits = new GameObject[4];
    // фрукты для выбора
    GameObject[] selectFruits = new GameObject[SELECT_FRUITS_COUNT];
    // вопросик
    GameObject question;
    // какой фрукт скрыт 0..3
    int hiddenFruit;
    // какой будет правильный ответ
    int goodAnswear;

    // игровые очки
    float score = 0, speedScore = 0;
    int realScore = 0;

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();

        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        uiCamera = new OrthographicCamera();

        // Настраиваю размеры сцены
        //
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        SCREEN_KOEF = 1f * SCREEN_WIDTH / w;
        SCREEN_HEIGHT = 1f * SCREEN_KOEF * h;
        camera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);
        uiCamera.setToOrtho(false, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Загрузка ресурсов
        //
        RF = new ResourceFactory(FRUITS_COUNT);

        // Создаю объекты
        //

        // где располагаются фрукты
        float _x = 20, _y = SCREEN_HEIGHT - 40 - 150;
        for (int i = 0; i < 4; i++) {
            showFruitsX[i] = _x + (i % 2) * 80;
            showFruits[i] = new GameObject();
            showFruits[i].setOrigin(35, 35);
            showFruits[i].setBounds(0, _y + (i / 2) * 80, 70, 70);
        }

        // где будут располагаться варианты ответов
        _x = 20;
        float dx = (SCREEN_WIDTH - _x * 2) / SELECT_FRUITS_COUNT;
        _y = (SCREEN_HEIGHT - _y) / 2 - (dx) / 2;
        for (int i = 0; i < SELECT_FRUITS_COUNT; i++) {
            selectFruits[i] = new GameObject();
            selectFruits[i].setBounds(_x + i * dx + 5, _y, dx - 10, dx - 10);
        }

        // вопросик
        question = new GameObject();
        question.setBounds(0, 0, 70, 70);

        // Начало игры
        //
        setStartState();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        update(Gdx.graphics.getDeltaTime());

        // отрисовка фона и UI
        uiCamera.update();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        {
            batch.draw(RF.background, uiCamera.position.x - RF.background.getWidth() / 2, 0);

            drawSelectFruits();
            drawBorder();
            drawScore();
        }
        batch.end();

        // отрисовка игровых объектов
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        {
            drawShowFruits();
        }
        batch.end();

        if (DEBUG) {
            drawDebug();
        }
    }

    void update(float deltaTime) {
        float maxTime = TIME_IN_STATE[gameState];
        if (stateDelta > maxTime)
            switch (gameState) {
                case STATE_START:
                    setComeState();
                    break;

                case STATE_COME:
                    setStayState();
                    break;

                case STATE_STAY:
                    setQuestionState();
                    break;

                case STATE_QUESTION:
                    setWrongState();
                    break;

                case STATE_WRONG:
                case STATE_GOOD:
                    setAwayState();
                    break;

                case STATE_AWAY:
                    setStartState();
                    break;
            }

        if (gameState == STATE_QUESTION) {
            updateQuestionState();
        }

        updateScore(deltaTime);

        stateDelta += deltaTime;
    }

    //-------------------------------------------
    //  Процесс
    //-------------------------------------------

    void updateQuestionState() {
        if (Gdx.input.justTouched()) {
            Vector3 tap = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(tap);

            // куда кликнули
            int clickIndex = -1;
            for (int i = 0; i < SELECT_FRUITS_COUNT; i++) {
                if (selectFruits[i].inBounds(tap.x, tap.y)) {
                    clickIndex = i;
                    break;
                }
            }

            if (clickIndex < 0)
                return;

            if (clickIndex == goodAnswear)
                setGoodState();
            else
                setWrongState();
        }
    }

    void updateScore(float deltaTime) {
        if (score == realScore)
            return;

        if (speedScore == 0) {
            speedScore = (realScore - score) / 1;
        }

        // скорость в секунду
        score += speedScore * deltaTime;
        if ((speedScore > 0 && score >= realScore)
                || (speedScore < 0 && score <= realScore)) {
            score = realScore;
            speedScore = 0;
        }
    }

    //-------------------------------------------
    //  Отрисовка
    //-------------------------------------------

    void drawShowFruits() {
        if (gameState == STATE_START)
            return;

        float maxTime = TIME_IN_STATE[gameState];
        float offsetx = 0;

        if (gameState == STATE_COME) {
            offsetx += SCREEN_WIDTH * (TIME_IN_STATE[gameState] - stateDelta) / maxTime;
        }

        if (gameState == STATE_AWAY) {
            offsetx -= SCREEN_WIDTH * stateDelta / maxTime;
        }

        for (int i = 0; i < showFruits.length; i++) {
            if (i == hiddenFruit && gameState == STATE_QUESTION) {
                question.setRegion(RF.question.getKeyFrame(stateDelta));
                question.draw(batch);
            } else {
                showFruits[i].setX(showFruitsX[i] - offsetx);
                showFruits[i].setRotation(offsetx * 1.5f);
                showFruits[i].draw(batch);
            }
        }
    }

    void drawSelectFruits() {
        if (gameState != STATE_QUESTION && gameState != STATE_WRONG && gameState != STATE_GOOD)
            return;

        for (GameObject selectFruit : selectFruits) {
            selectFruit.draw(batch);
        }
    }

    void drawBorder() {
        if (gameState != STATE_WRONG && gameState != STATE_GOOD)
            return;

        TextureRegion tr = (gameState == STATE_WRONG
                ? RF.wrongBorder.getKeyFrame(stateDelta)
                : RF.goodBorder.getKeyFrame(stateDelta));

        float x = 20,
                y = SCREEN_HEIGHT - 40 - 150,
                b = ((stateDelta * 2) % 1);

        batch.draw(tr, (hiddenFruit % 2) * 80 + x - b * 15, (hiddenFruit / 2) * 80 + y - b * 15,
                70 + b * 30, 70 + b * 30);
    }

    void drawScore() {
        String text = "" + (int) score;
        float width = text.length() * 20;
        RF.font.draw(batch, text, (SCREEN_WIDTH - width) / 2, RF.font.getLineHeight() + 5);
    }

    void drawDebug() {
        shapeRenderer.setColor(Color.BLACK);

        //
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setProjectionMatrix(camera.combined);
        {
            for (GameObject showFruit : showFruits) {
                Rectangle r = showFruit.getBoundingRectangle();
                shapeRenderer.rect(r.x, r.y, r.width, r.height);
            }
        }
        shapeRenderer.end();

        //
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        {
            for (int i = 0; i < SELECT_FRUITS_COUNT; i++) {
                Rectangle r = selectFruits[i].getBoundingRectangle();
                shapeRenderer.rect(r.x, r.y, r.width, r.height);
            }
        }
        shapeRenderer.end();

        shapeRenderer.setColor(Color.RED);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        {
            float px = Gdx.input.getX() * SCREEN_KOEF;
            float py = SCREEN_HEIGHT - Gdx.input.getY() * SCREEN_KOEF;
            shapeRenderer.circle(px, py, 5);
        }
        shapeRenderer.end();
    }

    //-------------------------------------------
    //  Изменение состояния игры
    //-------------------------------------------

    void setStartState() {
        gameState = STATE_START;
        stateDelta = 0;

        ArrayList<Integer> know = new ArrayList<Integer>();
        ArrayList<Integer> select = new ArrayList<Integer>();

        // загадываем фрукты
        for (int i = 0; i < 4; i++) {
            while (know.size() <= i) {
                int k = random.nextInt(FRUITS_COUNT);
                if (!know.contains(k)) {
                    know.add(k);
                    // текстура
                    showFruits[i].setRegion(RF.fruitSprites[k]);
                }
            }
        }

        // какой фрукт будем скрывать
        hiddenFruit = random.nextInt(4);

        // на каком месте будет скрытый фрукт среди вариантов выбора
        goodAnswear = random.nextInt(SELECT_FRUITS_COUNT);

        // какие фрукты будем предлагать для выбора
        for (int i = 0; i < SELECT_FRUITS_COUNT; i++) {
            if (i == goodAnswear) {
                int k = know.get(hiddenFruit);
                select.add(k);
                // текстура
                selectFruits[i].setRegion(RF.fruitSprites[k]);
            } else
                while (select.size() <= i) {
                    int k = random.nextInt(FRUITS_COUNT);
                    if (!know.contains(k) && !select.contains(k)) {
                        select.add(k);
                        // текстура
                        selectFruits[i].setRegion(RF.fruitSprites[k]);
                    }
                }
        }

        // где расположен будет вопрос
        question.setPosition(showFruitsX[hiddenFruit], showFruits[hiddenFruit].getY());
    }

    void setComeState() {
        gameState = STATE_COME;
        stateDelta = 0;
    }

    void setStayState() {
        gameState = STATE_STAY;
        stateDelta = 0;
    }

    void setQuestionState() {
        gameState = STATE_QUESTION;
        stateDelta = 0;
    }

    void setWrongState() {
        gameState = STATE_WRONG;
        stateDelta = 0;

        RF.wrongSound.play();
        realScore = Math.max(0, realScore - 200);
    }

    void setGoodState() {
        gameState = STATE_GOOD;
        stateDelta = 0;

        RF.goodSound.play();
        realScore += 100;
    }

    void setAwayState() {
        gameState = STATE_AWAY;
        stateDelta = 0;
    }
}