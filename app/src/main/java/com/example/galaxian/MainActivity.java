package com.example.galaxian;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements JGameLib.GameEvent {
    boolean isRunning = false;
    Point screenSize = new Point(100,160);
    JGameLib gameLib = null;
    JGameLib.Card gameBackground;
    JGameLib.Card cardAvatar;
    JGameLib.Card cardExplosion;
    Button btnRestart;
    float bulletSize1 = 5;
    float bulletSpeed = 2;
    float avatarSize = 10;
    float evemySize = 15;
    float enemySpeed = 1.5f;
    ArrayList<JGameLib.Card> enemies = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnRestart = findViewById(R.id.btnRestart);
        gameLib = findViewById(R.id.gameLib);
        initGame();
    }

    @Override
    protected void onDestroy() {
        if (gameLib != null)
            gameLib.clearMemory();
        super.onDestroy();
    }

    void initGame() {
        gameLib.setScreenGrid(screenSize.x, screenSize.y);
        gameLib.listener(this);
        newGame();
    }

    void newGame() {
        enemies = new ArrayList();
        gameLib.clearMemory();
        gameBackground = gameLib.addCard(R.drawable.scroll_back_galaxy);
        gameBackground.sourceRect(0, 50, 100, 50);
        cardAvatar = gameLib.addCard(R.drawable.spaceship_up01, 50-avatarSize/2,140,avatarSize,avatarSize);
        cardAvatar.checkCollision();
        for(int j=10; j <= 30; j+=20) {
            for (int i = 8; i < 90; i += 23) {
                JGameLib.Card enemy = gameLib.addCard(R.drawable.spaceship_down01, i, j, evemySize, evemySize);
                enemy.set(1);
                enemy.checkCollision();
                enemy.autoRemove();
                enemies.add(enemy);
            }
        }
    }

    void startGame() {
        cardExplosion = gameLib.addCard(R.drawable.explosion01,0,0,20,20);
        cardExplosion.visible(false);
        cardExplosion.addImage(R.drawable.explosion02);
        cardExplosion.addImage(R.drawable.explosion03);
        cardExplosion.addImage(R.drawable.explosion04);
        cardExplosion.addImage(R.drawable.explosion05);
        cardExplosion.addImage(R.drawable.explosion06);
        scrollBackground();
        enemyStart();
    }

    void enemyStart() {
        if(enemies.isEmpty()) return;
        JGameLib.Card enemy = enemies.get(enemies.size()-1);
        enemy.set(2);
        enemy.movingSpeed(80,35, enemySpeed);
    }

    void scrollBackground() {
        gameBackground.sourceRect(0, 50, 100, 50);
        gameBackground.sourceRectIng(0, -50, 25);
    }

    void startBulletUp(JGameLib.Card card) {
        RectF rectMe = card.screenRect();
        float t = rectMe.top - bulletSize1 - 1;
        float l = rectMe.centerX() - (bulletSize1/2);
        JGameLib.Card bullet = gameLib.addCard(R.drawable.bullet_up01, l, t, bulletSize1, bulletSize1);
        bullet.checkCollision();
        bullet.autoRemove();
        bullet.movingDir(0, -bulletSpeed);
    }

    void startBulletDown() {
        if(enemies.size() < 1) return;
        JGameLib.Card enemy = enemies.get(enemies.size()-1);
        startBulletDown(enemy);
    }

    void startBulletDown(JGameLib.Card card) {
        RectF rectMe = card.screenRect();
        float t = rectMe.bottom + 2;
        float l = rectMe.centerX() - (bulletSize1/2);
        JGameLib.Card bullet = gameLib.addCard(R.drawable.bullet_down01, l, t, bulletSize1, bulletSize1);
        bullet.checkCollision();
        bullet.autoRemove();
        bullet.movingDir(0, bulletSpeed);
    }

    void stopGame(boolean win) {
        isRunning = false;
        btnRestart.setText("Restart");
        gameLib.stopTimer();
        gameLib.stopAllWork();
        String message = "You loose. Try again";
        if(win)
            message = "Congratulation! You win. Try again";
        gameLib.popupDialog(null, message, "Close");
    }

    int findEnemy(JGameLib.Card card) {
        for(int i = enemies.size()-1; i >= 0; i--) {
            JGameLib.Card enemy = enemies.get(i);
            if(enemy == card)
                return i;
        }
        return -1;
    }

    void removeEnemy(int idx) {
        if(idx < 0 || idx >= enemies.size()) return;
        enemies.remove(idx);
    }

    void startExplosion(float centerX, float centerY) {
        float l = centerX - cardExplosion.screenRect().width()/2;
        float t = centerY - cardExplosion.screenRect().height()/2;
        cardExplosion.move(l, t);
        cardExplosion.visible(true);
        cardExplosion.imageChanging(1);
    }

    // User Event start ====================================

    public void onBtnRestart(View v) {
        if(isRunning) {
            startBulletUp(cardAvatar);
        } else {
            isRunning = true;
            btnRestart.setText("Fire");
            gameLib.startTimer(1.6);
            newGame();
            startGame();
        }
    }

    public void onBtnArrow(View v) {
        double moveUnit = 7;
        RectF rect = cardAvatar.screenRect();
        switch(v.getId()) {
            case R.id.btnLeft:
                if(rect.left < moveUnit)
                    moveUnit = -rect.left;
                else
                    moveUnit = -moveUnit;
                break;
            case R.id.btnRight:
                if(rect.right+moveUnit > screenSize.x)
                    moveUnit = screenSize.x - rect.right;
        }
        cardAvatar.moveGap(moveUnit, 0);
    }

    // User Event end ====================================

    // Game Event start ====================================

    @Override
    public void onGameWorkEnded(JGameLib.Card card, JGameLib.WorkType workType) {
        switch(workType) {
            case SOURCE_RECT: {
                if(card == gameBackground) {
                    scrollBackground();
                }
                break;
            }
            case MOVE: {
                switch(card.getInt()) {
                    case 2:
                        card.movingSpeed(0,144, enemySpeed);
                        card.set(3);
                        break;
                    case 3:
                        removeEnemy(enemies.size()-1);
                        gameLib.removeCard(card);
                        enemyStart();
                        break;
                }
                break;
            }
            case IMAGE_CHANGE:
                if(card == cardExplosion) {
                    cardExplosion.visible(false);
                    if(cardAvatar == null) {
                        stopGame(false);
                    } else if(enemies.isEmpty()) {
                        stopGame(true);
                    }
                }
                break;
        }
    }

    @Override
    public void onGameTouchEvent(JGameLib.Card card, int action, float blockX, float blockY) {}

    @Override
    public void onGameSensor(int sensorType, float x, float y, float z) {}

    @Override
    public void onGameCollision(JGameLib.Card card1, JGameLib.Card card2) {
        if(card1 == cardAvatar) {
            startExplosion(cardAvatar.screenRect().centerX(), cardAvatar.screenRect().centerY());
            cardAvatar = null;
            gameLib.removeCard(card1);
            gameLib.removeCard(card2);
        } else {
            int idx = findEnemy(card1);
            if(idx >= 0) {
                startExplosion(card1.screenRect().centerX(), card1.screenRect().centerY());
                removeEnemy(idx);
                gameLib.removeCard(card1);
                gameLib.removeCard(card2);
            }
            int remain = enemies.size();
            if(remain > 0 && enemies.get(remain-1).getInt() <= 1) {
                enemyStart();
            }
        }
    }

    @Override
    public void onGameTimer(int what) {
        if(enemies.isEmpty()) {
            if(isRunning)
                stopGame(true);
        } else {
            startBulletDown();
        }
    }

    // Game Event end ====================================

}