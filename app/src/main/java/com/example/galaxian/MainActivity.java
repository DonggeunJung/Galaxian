package com.example.galaxian;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Mosaic.GameEvent {
    Button btnRestart;
    boolean isRunning = false;
    Point screenSize = new Point(100,160);
    Mosaic mosaic = null;
    Mosaic.Card gameBackground, cardAvatar, cardExplosion;
    final float bulletSize1 = 5, bulletSpeed = 2, avatarSize = 10;
    final float evemySize = 12, enemySpeed = 1.5f;
    ArrayList<Mosaic.Card> enemies = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnRestart = findViewById(R.id.btnRestart);
        mosaic = findViewById(R.id.mosaic);
        initGame();
    }

    @Override
    protected void onDestroy() {
        if (mosaic != null)
            mosaic.clearMemory();
        super.onDestroy();
    }

    void initGame() {
        mosaic.listener(this);
        mosaic.setScreenGrid(screenSize.x, screenSize.y);
        newGame();
    }

    void newGame() {
        enemies = new ArrayList();
        mosaic.clearMemory();
        gameBackground = mosaic.addCard(R.drawable.scroll_back_galaxy);
        gameBackground.sourceRect(0, 50, 100, 50);
        cardAvatar = mosaic.addCard(R.drawable.spaceship_up01, 50-avatarSize/2,140,avatarSize,avatarSize);
        cardAvatar.checkCollision();
        for(int j=10; j <= 30; j+=20) {
            for (int i = 8; i < 90; i += 23) {
                Mosaic.Card enemy = mosaic.addCard(R.drawable.spaceship_down01, i, j, evemySize, evemySize);
                enemy.set(1);
                enemy.checkCollision();
                enemy.autoRemove();
                enemies.add(enemy);
            }
        }
    }

    void startGame() {
        cardExplosion = mosaic.addCard(R.drawable.explosion01,0,0,20,20);
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
        Mosaic.Card enemy = enemies.get(enemies.size()-1);
        enemy.set(2);
        enemy.movingSpeed(80,35, enemySpeed);
    }

    void scrollBackground() {
        gameBackground.sourceRect(0, 50, 100, 50);
        gameBackground.sourceRectIng(0, -50, 25);
    }

    void startBulletUp(Mosaic.Card card) {
        RectF rectMe = card.screenRect();
        float t = rectMe.top - bulletSize1 - 1;
        float l = rectMe.centerX() - (bulletSize1/2);
        Mosaic.Card bullet = mosaic.addCard(R.drawable.bullet_up01, l, t, bulletSize1, bulletSize1);
        bullet.checkCollision();
        bullet.autoRemove();
        bullet.movingDir(0, -bulletSpeed);
    }

    void startBulletDown() {
        if(enemies.size() < 1) return;
        Mosaic.Card enemy = enemies.get(enemies.size()-1);
        startBulletDown(enemy);
    }

    void startBulletDown(Mosaic.Card card) {
        RectF rectMe = card.screenRect();
        float t = rectMe.bottom + 2;
        float l = rectMe.centerX() - (bulletSize1/2);
        Mosaic.Card bullet = mosaic.addCard(R.drawable.bullet_down01, l, t, bulletSize1, bulletSize1);
        bullet.checkCollision();
        bullet.autoRemove();
        bullet.movingDir(0, bulletSpeed);
    }

    void stopGame(boolean win) {
        isRunning = false;
        btnRestart.setText("Restart");
        mosaic.stopTimer();
        mosaic.stopAllWork();
        String message = "You loose. Try again";
        if(win)
            message = "Congratulation! You win. Try again";
        mosaic.popupDialog(null, message, "Close");
    }

    int findEnemy(Mosaic.Card card) {
        for(int i = enemies.size()-1; i >= 0; i--) {
            Mosaic.Card enemy = enemies.get(i);
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
            mosaic.startTimer(1.6);
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
    public void onGameWorkEnded(Mosaic.Card card, Mosaic.WorkType workType) {
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
                        mosaic.removeCard(card);
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
    public void onGameTouchEvent(Mosaic.Card card, int action, float x, float y) {}

    @Override
    public void onGameSensor(int sensorType, float x, float y, float z) {}

    @Override
    public void onGameCollision(Mosaic.Card card1, Mosaic.Card card2) {
        if(card1 == cardAvatar) {
            startExplosion(cardAvatar.screenRect().centerX(), cardAvatar.screenRect().centerY());
            cardAvatar = null;
            mosaic.removeCard(card1);
            mosaic.removeCard(card2);
        } else {
            int idx = findEnemy(card1);
            if(idx >= 0) {
                startExplosion(card1.screenRect().centerX(), card1.screenRect().centerY());
                removeEnemy(idx);
                mosaic.removeCard(card1);
                mosaic.removeCard(card2);
            }
            int remain = enemies.size();
            if(remain > 0 && enemies.get(remain-1).getInt() <= 1) {
                enemyStart();
            }
        }
    }

    @Override
    public void onGameTimer() {
        if(enemies.isEmpty()) {
            if(isRunning)
                stopGame(true);
        } else {
            startBulletDown();
        }
    }

    // Game Event end ====================================

}