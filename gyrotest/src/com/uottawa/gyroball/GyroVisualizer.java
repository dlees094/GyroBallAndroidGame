package com.uottawa.gyroball;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.media.MediaPlayer;

public class GyroVisualizer extends View {

    private Paint mAccPaint = new Paint();
    private Paint targetPaint = new Paint();
    private Paint textPaint = new Paint();
    private Paint holePaint = new Paint();
    private List<Position> holes;
    private float fairnessMultiplier = 0;
    private BluetoothGameService bluetoothService;
    private boolean active;   
	
	private boolean singleMode;
 
    private MediaPlayer pointSound;
    private MediaPlayer loseSound;
    
    private int rank;
    private boolean quit;
    private Timer timer;
    private int interval;
    
    private float mAccX, mAccY;
    private float velocityX, velocityY;
    private int points = 0;
    private float targetPosX = 0;
    private float targetPosY = 0;
    private float previousX, previousY;
    
    private BallTiltWebServiceClient webService;
	private Context context;
	
    public float getPreviousY() {
		return previousY;
	}

	public void setPreviousY(float previousY) {
		this.previousY = previousY;
	}

	public void setPreviousX(float previousX) {
		this.previousX = previousX;
	}

    public void setVelocityX(float velocityX) {
		this.velocityX = velocityX;
	}

	public void setVelocityY(float velocityY) {
		this.velocityY = velocityY;
	}

	
	public GyroVisualizer(Context context, boolean activate, boolean singleMode, BluetoothGameService bluetoothService) {
        this(context, null);
        mAccPaint.setColor(0xff33bb33);
        mAccPaint.setStrokeWidth(5);
        mAccPaint.setAntiAlias(true);
        
        rank = -1;
        quit = false;
        timer = new Timer();
        interval = 60;
        int delay = 1000;
        int period = 1000;
        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
            	if (interval == 1)
            	{
                    timer.cancel();
                    quit = true;
                    quit();
            	}
                --interval;
            }
        }, delay, period);
        
        this.context = context;
        
        previousX = -100;
        previousY = -100;
        velocityX = 0;
        velocityY = 0;
	    active = activate;
	    this.singleMode = singleMode;
	    this.bluetoothService = bluetoothService;
	}

    public GyroVisualizer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GyroVisualizer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        webService = BallTiltWebServiceClient.getInstance();
        
        mAccPaint.setColor(0xff33bb33);
        mAccPaint.setStrokeWidth(5);
        mAccPaint.setAntiAlias(true);
        
        targetPaint.setColor(0xffff0000);
        targetPaint.setStrokeWidth(5);
        targetPaint.setAntiAlias(true);
        
        holePaint.setColor(0xffff00ff);
        holePaint.setStrokeWidth(5);
        holePaint.setAntiAlias(true);
        
        textPaint.setColor(0xffffff00);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(22);
        
        holes = new ArrayList<Position>();
        previousX = -100;
        previousY = -100;
        velocityX = 0;
        velocityY = 0;
        
        loseSound = MediaPlayer.create(this.getContext(), R.raw.end);
    	loseSound.setVolume(100, 100);
    	pointSound = MediaPlayer.create(this.getContext(), R.raw.point);
    	pointSound.setVolume(100, 100); 
    }

    @Override
    protected void onDraw(Canvas canvas) {
    	if (active)
    	{
    		if (fairnessMultiplier == 0)
    		{
    			if (getWidth() > 0 && getWidth() < 500)
    				fairnessMultiplier = 1;
    			else if (getWidth() >= 500 && getWidth() < 1000)
    				fairnessMultiplier = 2;
    			else
    				fairnessMultiplier = 3;
    	        textPaint.setTextSize(22*fairnessMultiplier);
    		}
	    	super.onDraw(canvas);
	    	if (previousX == -100 && previousY == -100)
	    	{
		    	previousX = getWidth() / 2f;
		        previousY = getHeight() / 2f;
	    	}
	    	
	        velocityX += mAccX/20f;
	        velocityY += mAccY/20f;
	        // Accelerometer
	        previousX += velocityX;
	        previousY += velocityY;
	        
	        checkBoundaries();
	        
	        checkHoles();
	        
	        checkTarget();
			//int v = (int) (Math.pow(Math.pow(velocityX, 2) + Math.pow(velocityY, 2), 0.5) / 100/*MAX_SPEED*/ * 100/*MAX_VOLUME*/);
			//mp.setVolume(v, v);
	        
	        canvas.drawCircle(previousX, previousY, 15 * fairnessMultiplier, mAccPaint);

	        canvas.drawCircle(targetPosX, targetPosY, 10 * fairnessMultiplier, targetPaint);
	        
	        for (int x = 0; x < holes.size(); x++)
	    	{
	        	canvas.drawCircle(holes.get(x).getX(), holes.get(x).getY(), 5 * fairnessMultiplier, holePaint);
	    	}
	        
	        canvas.drawText(String.valueOf(points), getWidth()/2, getHeight()/11, textPaint);
	        canvas.drawText(String.valueOf(interval), getWidth()/2-10, getHeight()/11 + 20, textPaint);
	        
	        invalidate();
    	}
    	else
    	{
	    	super.onDraw(canvas);
	        canvas.drawText(String.valueOf(points), getWidth()/2, getHeight()/11, textPaint);
	        String a;
	        if (quit)
    			a = "Rank was: " + rank;
	        else
	        	a = "Waiting for other player";
    		canvas.drawText(a, (float) (getWidth()/2-(a.length()*5)), getHeight()/2, textPaint);	        
	        invalidate();
    	}
    }

    public void setAcceleration(float x, float y) {
        mAccX = x;
        mAccY = y;
    }
    
    public void checkBoundaries()
    {        
        if (previousX < 0 || previousX > getWidth() || previousY < 0 || previousY > getHeight())
        {
        	lose();
        }
        
    }
    
    void lose(){
	
    	makeSound();
    	
    	if (previousX < 0) previousX = 0.99999F;                	
        else if ( previousX > this.getWidth() ) previousX = 0.00001F;
        else previousX = 1 - (previousX / getWidth());
        
        if (previousY < 0) previousY = 0.99999F;
        else if ( previousY > getHeight() ) previousY = 0.00001F;
        else previousY = 1 - (previousY / getHeight());
        this.velocityX /= 1.25;
        this.velocityY /= 1.25;
        
		if(singleMode) {
            previousX *= getWidth();
            previousY *= getHeight();
		} else {
			
            active = false;
            sendMessage(previousX + "," + previousY + "," + (velocityX) + "," + ( velocityY) + "," + getWidth() + "," + getHeight() );
			
		}
	}
    
	private void quit() {
		active = false;
		try {
			rank = webService.submitScore(points);
		}
		catch (Exception e){e.printStackTrace();}
	}
    
    void checkTarget(){

    	if (targetPosX == 0 && targetPosY == 0)
    	{
    		generateNewTarget();
    	}
    	
    	if ( Math.abs(previousX - targetPosX) < 20 * fairnessMultiplier && Math.abs(previousY - targetPosY) < 20 * fairnessMultiplier){
    		points++;
    		generateNewTarget();
    		makePointSound();
    		if (points % 3 == 0)
    			generateHole();
    	}
    	
    }

    void checkHoles(){
    	for (int x = 0; x < holes.size(); x++)
    	{
    		if ( Math.abs(previousX - holes.get(x).getX()) < 15 * fairnessMultiplier && Math.abs(previousY - holes.get(x).getY()) < 15 * fairnessMultiplier ){
    			lose();
    		}
    	}
    }
    
    void generateNewTarget(){

    	targetPosX = (float) (Math.random() * getWidth());
    	targetPosY = (float) (Math.random() * getHeight());
    	
    }
    
    void generateHole(){

    	holes.add(new Position((float)(Math.random() * getWidth()), (float)(Math.random() * getHeight())));
    	
    }
    
    void makeSound(){
    	try{
	    	loseSound.start();
    	}
    	catch(Exception e){}
    }
    
    void makePointSound(){
    	try{
	    	pointSound.start();
		}
		catch(Exception e){}
    }

    void setActive(boolean on){
    	active = on;
    }
	
	   /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (bluetoothService.getState() != BluetoothGameService.STATE_CONNECTED) {
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            bluetoothService.write(send);
        }
    }
}
