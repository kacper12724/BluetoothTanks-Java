import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.ThreadLocalRandom;
  
import javax.bluetooth.*;
import javax.microedition.io.*;

import Rodzaje.*;


public class Board extends JPanel implements MouseListener, ActionListener, KeyListener, Runnable{
	private static final long serialVersionUID = 1L;

//Pola planszy	
	private Field [][] fields = new Field[100][100];
	private static int size = 7;

//Kolory
	private static Color puste = Color.WHITE;
	private static Color pocisk = Color.RED;
	private static Color mur = Color.BLACK;
	private static Color zabudowanie = Color.GREEN;
	private static Color msc_eksplozji = Color.PINK;
	private static Color eksplozja = Color.ORANGE;
	
	private static Color czolg = Color.BLUE;
	private static Color czolg_skrzydlo = Color.GRAY;

//Gracze
	private Player gracz = Player.g1_c;
	private static Color p1 = Color.WHITE;
	private static Color p2 = Color.BLACK;
	private DisplayPlayer cruiserDisplayTaken = new DisplayPlayer(750, 0, 30, 30, Color.BLACK);
	private DisplayPlayer cruiserDisplayActive = new DisplayPlayer(780, 0, 30, 30, Color.BLACK);
	private Field [] ammoTableBullet = new Field[30];//tablica z liczba amunicji
	private Field [] ammoTableNuke = new Field[30];
	private Field [] ammoTableGauss = new Field[30];
	private int ammoCounterBullet = 10;//licznik amunicji
	private int ammoCounterNuke = 5;
	private int ammoCounterGauss = 10;
	private int lifeCounter = 1;//licznik sprawdzajacy czy zyjemy
	private int baseLifeCounter = 11;
	private int heliCtBg = 0;//zewnetrzna petla ruchu helikoptera
	private int heliCtLw = 0;//wewnetrzna petla ruchu helikoptera
	private int boss2Counter = 18;
	private int ueCounter = 0;//kolejne poziomy wybuchy UE
	private boolean heliLive = true;
	private boolean baseVisited = false;
	private boolean boss2Live = true;
	private boolean cruiserPowerupCollected = false;//zebranie cruiser powerup
	private boolean iChooseCruiser = false;//aktywacja cruisera poprzez andro
	private boolean crosshair = false;
	
//Pociski
	private TypeOfProjectile typPociskuGracza = TypeOfProjectile.bullet;
	private TypeOfProjectile typPociskuKomputera = TypeOfProjectile.bullet;
	private Player typeOfTank = Player.unarmoured_tank;
	private DisplayProjectile[] projectiles = new DisplayProjectile[3];
	private Direction direction = Direction.dirL;
	private Direction heliDirection = Direction.dirL;
	private int ran;//losowy wybuch nuke
	private int ran2;
	private int speedOfBoss = 93;
	private int bossCounter = 0;
	private int cruiserPosCounter = 0;

//Timer	
	Timer timer=new Timer(70, this);
	
	public int tura = 3;
	public int blokada = 1;//najpierw stawia serwer dopiero potem nasluchuj
	public int behavior = 0;
	
//SPP
	UUID uuid = new UUID("1101", true);
    String connectionString = "btspp://localhost:" + uuid +";name=Sample SPP Server";
    StreamConnectionNotifier streamConnNotifier;
    StreamConnection connection;
    InputStream inStream;
    BufferedReader bReader;
    String lineRead;
	
	public Board(){
		
		for(int i=0; i<100; i++)
			for(int j=0; j<100; j++)
				fields[i][j] = new Field(j*size, i*size, size, size, puste);
		
		projectiles[0] = new DisplayProjectile(750, 100, 20, 20, pocisk);
		projectiles[1] = new DisplayProjectile(750, 150, 40, 40, pocisk);
		projectiles[2] = new DisplayProjectile(750, 200, 5, 30, pocisk);
		
		for(int i=0; i<=ammoCounterBullet; i++)
			ammoTableBullet[i] = new Field(810+(i*size)+i, 100, size, size, Color.BLACK);
		for(int i=0; i<=ammoCounterNuke; i++)
			ammoTableNuke[i] = new Field(810+(i*size)+i, 150, size, size, Color.BLACK);
		for(int i=0; i<=ammoCounterGauss; i++)
			ammoTableGauss[i] = new Field(810+(i*size)+i, 200, size, size, Color.BLACK);
		
		TriggerFields();
		this.addMouseListener(this);
		this.addKeyListener(this);
		Gra();//pierwsze narysowanie planszy
		timer.start();//tutaj odswiezanie planszy
		new Thread(new Runnable() {
		    public void run() {
		    	while(true){
		    		//Move();
		    		try {
		    				System.out.println("W1-listen");
		    		        //Move();// this will call at every x second
		    		        if (blokada==2){
		    		        	sluchaj();
		    		        }
		    		        if (blokada==1){
		    		        	try{
		    		        		blokada = 2;
		    						initBlue();
		    						} catch(Exception ee){
		    							System.out.println("error in timer");
		    						}
		    		        }
		            } catch (Exception e) {
		                e.printStackTrace();
		            }
		    	}
		    }
		}).start();
        
	}
	
	public void initBlue() throws IOException{
        
        //open server url
        streamConnNotifier = (StreamConnectionNotifier)Connector.open( connectionString );
        
        //Wait for client connection
        System.out.println("\nServer Started. Waiting for clients to connect...");
        connection=streamConnNotifier.acceptAndOpen();
  
        RemoteDevice dev = RemoteDevice.getRemoteDevice(connection);
        System.out.println("Remote device address: "+dev.getBluetoothAddress());
        System.out.println("Remote device name: "+dev.getFriendlyName(true));
        //read string from spp client        
        inStream=connection.openInputStream();
        bReader=new BufferedReader(new InputStreamReader(inStream));
	}
	
	@Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		g2d.setColor(fields[i][j].getColor());
                g2d.fill(fields[i][j].getBounds());
        	}
        g2d.setColor(cruiserDisplayTaken.getColor());//czy cruiser zebrany
        g2d.fill(cruiserDisplayTaken.getBounds());
        g2d.setColor(cruiserDisplayActive.getColor());//czy cruiser aktywny
        g2d.fill(cruiserDisplayActive.getBounds());
        
        for (int i=0; i<3; i++){//rodzaj pocisku
        	g2d.setColor(projectiles[i].getColor());
        	g2d.fill(projectiles[i].getBounds());
        }
        for (int i=0; i<=ammoCounterBullet; i++){//licznik pocisków
        	g2d.setColor(ammoTableBullet[i].getColor());
        	g2d.fill(ammoTableBullet[i].getBounds());
        }
        for (int i=0; i<=ammoCounterNuke; i++){//licznik pocisków
        	g2d.setColor(ammoTableNuke[i].getColor());
        	g2d.fill(ammoTableNuke[i].getBounds());
        }
        for (int i=0; i<=ammoCounterGauss; i++){//licznik pocisków
        	g2d.setColor(ammoTableGauss[i].getColor());
        	g2d.fill(ammoTableGauss[i].getBounds());
        }
        
        g2d.dispose();
	
    }

	@Override
	public void mousePressed(MouseEvent click) {


	}
	
	public void sluchaj(){
		
		try{
		lineRead=bReader.readLine();
    	System.out.println(lineRead);
		}catch (Exception e) {System.out.println("sluchaj error");}
		switch (lineRead) {
		case "FWD"://ruch do przodu
			try{
				for (int i=0;i<100;i++)
					for (int j=0;j<100;j++){
						if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i-1][j].setRodzajPola(RodzajPola.tank);
						}
					}
				} catch (Exception e){}
			direction = Direction.dirU;
			break;
		case "LFT"://ruch w lewo
			try{
				for (int i=0;i<100;i++)
					for (int j=0;j<100;j++){
						if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j-1].setRodzajPola(RodzajPola.tank);
						}
					}
				} catch (Exception e){}
			direction = Direction.dirL;
			break;
		case "BWD"://ruch do tylu
			try{
				for (int i=99;i>=0;i--)
					for (int j=99;j>=0;j--){
						if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i+1][j].setRodzajPola(RodzajPola.tank);
						}
					}
				} catch (Exception e){}
			direction = Direction.dirD;
			break;
		case "RGT"://ruch w prawo
			try{
				for (int i=99;i>=0;i--)
					for (int j=99;j>=0;j--){
						if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j+1].setRodzajPola(RodzajPola.tank);
						}
					}
				} catch (Exception e){}
			direction = Direction.dirR;
			break;
		case "WPNSW"://zmiana broni
			if (typPociskuGracza == TypeOfProjectile.gauss){
				typPociskuGracza = TypeOfProjectile.bullet;
			} else if (typPociskuGracza == TypeOfProjectile.bullet){
				typPociskuGracza = TypeOfProjectile.nuke;
			} else if (typPociskuGracza == TypeOfProjectile.nuke){
				typPociskuGracza = TypeOfProjectile.gauss;
			}
			break;
		case "CRZSW"://aktywacja cruisera
			if (cruiserPowerupCollected == true){
				if (iChooseCruiser == false){
					iChooseCruiser = true;
					cruiserDisplayActive.setColor(Color.YELLOW);
				} else if (iChooseCruiser == true){
					iChooseCruiser = false;
					cruiserDisplayActive.setColor(Color.BLACK);
				}
			} else if (cruiserPowerupCollected == false){
				iChooseCruiser = false;
			}
			break;
		case "ARMSW":
			if (typeOfTank == Player.unarmoured_tank){
				typeOfTank = Player.armoured_tank;
			} else if (typeOfTank == Player.armoured_tank){
				typeOfTank = Player.unarmoured_tank;
			}
			break;
		case "UE":
			ueCounter = 9;
			break;
		case "CRSHR":
			if (crosshair == false){
				crosshair = true;
			} else if (crosshair == true){
				crosshair = false;
			}
			break;
		case "SHT"://w zaleznosci od strony w ktora jestesmy zwroceni strzeli danym pociskiem
			switch (typPociskuGracza){
			case bullet:
				if (ammoCounterBullet != 0)
				ammoCounterBullet--;
				break;
			case nuke:
				if (ammoCounterNuke != 0)
				ammoCounterNuke--;
				break;
			case gauss:
				if (ammoCounterGauss != 0)
				ammoCounterGauss--;
				break;
			default:
				break;
			}
			switch (direction){
			case dirU:
			  if ((typPociskuGracza == TypeOfProjectile.bullet && ammoCounterBullet != 0) || (typPociskuGracza == TypeOfProjectile.nuke && ammoCounterNuke != 0) || (typPociskuGracza == TypeOfProjectile.gauss && ammoCounterGauss != 0)){
				try{
					for (int i=0;i<100;i++)
						for (int j=0;j<100;j++){
							if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
								fields[i-2][j].setRodzajPola(RodzajPola.missile_u);
							}
						}
					} catch (Exception e){}
			  }
				break;
			case dirD:
			  if ((typPociskuGracza == TypeOfProjectile.bullet && ammoCounterBullet != 0) || (typPociskuGracza == TypeOfProjectile.nuke && ammoCounterNuke != 0) || (typPociskuGracza == TypeOfProjectile.gauss && ammoCounterGauss != 0)){
				try{
					for (int i=0;i<100;i++)
						for (int j=0;j<100;j++){
							if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
								fields[i+2][j].setRodzajPola(RodzajPola.missile_d);
							}
						}
					} catch (Exception e){}
			  }
				break;
			case dirL:
		      if ((typPociskuGracza == TypeOfProjectile.bullet && ammoCounterBullet != 0) || (typPociskuGracza == TypeOfProjectile.nuke && ammoCounterNuke != 0) || (typPociskuGracza == TypeOfProjectile.gauss && ammoCounterGauss != 0)){
				try{
					for (int i=0;i<100;i++)
						for (int j=0;j<100;j++){
							if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
								fields[i][j-2].setRodzajPola(RodzajPola.missile_l);
							}
						}
					} catch (Exception e){}
		      }
				break;
			case dirR:
			  if ((typPociskuGracza == TypeOfProjectile.bullet && ammoCounterBullet != 0) || (typPociskuGracza == TypeOfProjectile.nuke && ammoCounterNuke != 0) || (typPociskuGracza == TypeOfProjectile.gauss && ammoCounterGauss != 0)){
				try{
					for (int i=0;i<100;i++)
						for (int j=0;j<100;j++){
							if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
								fields[i][j+2].setRodzajPola(RodzajPola.missile_r);
							}
						}
					} catch (Exception e){}
			  }
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}
		//Move();
	}
	
	public void UltimateExplosion(){
		if (ueCounter == 9){
			try{//UE
				for (int i=0;i<100;i++)
					for (int j=0;j<100;j++){
						if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
							fields[i-9][j].setRodzajPola(RodzajPola.to_explode);//3rd
							fields[i+9][j].setRodzajPola(RodzajPola.to_explode);
							fields[i][j-9].setRodzajPola(RodzajPola.to_explode);
							fields[i][j+9].setRodzajPola(RodzajPola.to_explode);
							fields[i+5][j-5].setRodzajPola(RodzajPola.to_explode);
							fields[i-5][j+5].setRodzajPola(RodzajPola.to_explode);
	        				fields[i-5][j-5].setRodzajPola(RodzajPola.to_explode);
	        				fields[i+5][j+5].setRodzajPola(RodzajPola.to_explode);
	        				fields[i+7][j-2].setRodzajPola(RodzajPola.to_explode);
	        				fields[i+7][j+2].setRodzajPola(RodzajPola.to_explode);
	        				fields[i-7][j-2].setRodzajPola(RodzajPola.to_explode);
	        				fields[i-7][j+2].setRodzajPola(RodzajPola.to_explode);
	        				fields[i+2][j-7].setRodzajPola(RodzajPola.to_explode);
	        				fields[i+2][j+7].setRodzajPola(RodzajPola.to_explode);
	        				fields[i-2][j-7].setRodzajPola(RodzajPola.to_explode);
	        				fields[i-2][j+7].setRodzajPola(RodzajPola.to_explode);
						}
					}
				} catch (Exception e){}
		} else if (ueCounter == 6){
				try{//UE
					for (int i=0;i<100;i++)
			        	for (int j=0;j<100;j++){
			        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
			        			fields[i-3][j-3].setRodzajPola(RodzajPola.to_explode);//2nd
			        			fields[i-3][j+3].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+3][j-3].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+3][j+3].setRodzajPola(RodzajPola.to_explode);
			        			fields[i-5][j-1].setRodzajPola(RodzajPola.to_explode);
			        			fields[i-5][j+1].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+5][j-1].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+5][j+1].setRodzajPola(RodzajPola.to_explode);
			        			fields[i-1][j-5].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+1][j+5].setRodzajPola(RodzajPola.to_explode);
			        			fields[i-1][j-5].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+1][j+5].setRodzajPola(RodzajPola.to_explode);
			        		}
			        	}
					} catch (Exception e){}
		} else if (ueCounter == 3){
				try{//UE
					for (int i=0;i<100;i++)
			        	for (int j=0;j<100;j++){
			        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
			        			fields[i-3][j].setRodzajPola(RodzajPola.to_explode);//1st
			        			fields[i-6][j].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+3][j].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+6][j].setRodzajPola(RodzajPola.to_explode);
			        			fields[i][j-3].setRodzajPola(RodzajPola.to_explode);
			        			fields[i][j-6].setRodzajPola(RodzajPola.to_explode);
			        			fields[i][j+3].setRodzajPola(RodzajPola.to_explode);
			        			fields[i][j+6].setRodzajPola(RodzajPola.to_explode);
			        			fields[i-2][j-2].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+2][j+2].setRodzajPola(RodzajPola.to_explode);
			        			fields[i-2][j+2].setRodzajPola(RodzajPola.to_explode);
			        			fields[i+2][j-2].setRodzajPola(RodzajPola.to_explode);
			        		}
			        	}
					} catch (Exception e){}
		}
		
	}//endof UltimateExplosion()
	
	public void Explosion(){
		try{//eksplozja
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.to_explode)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        			fields[i][j+1].setRodzajPola(RodzajPola.explosion);
	        			fields[i+1][j].setRodzajPola(RodzajPola.explosion);
	        			fields[i][j-1].setRodzajPola(RodzajPola.explosion);
	        			fields[i-1][j].setRodzajPola(RodzajPola.explosion);
	        		}
	        	}
			} catch (Exception e){			
			}
	}
	
	public void CollectPowerups(){
		try{//collect cruiser powerup
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && ((fields[i+1][j+1].getRodzajPola()==RodzajPola.cruiser_powerup)
							  												  || (fields[i+1][j-1].getRodzajPola()==RodzajPola.cruiser_powerup)
							  												  || (fields[i-1][j-1].getRodzajPola()==RodzajPola.cruiser_powerup)
							  												  || (fields[i-1][j+1].getRodzajPola()==RodzajPola.cruiser_powerup)
							  												  || (fields[i][j+1].getRodzajPola()==RodzajPola.cruiser_powerup)
							  												  || (fields[i][j-1].getRodzajPola()==RodzajPola.cruiser_powerup)
							  												  || (fields[i-1][j].getRodzajPola()==RodzajPola.cruiser_powerup)
							  												  || (fields[i+1][j].getRodzajPola()==RodzajPola.cruiser_powerup)))
	        		{
	        			cruiserPowerupCollected = true;	
	        			cruiserDisplayTaken.setColor(Color.YELLOW);
	        		}
	        	}
			} catch (Exception e){}
		try{//collect ammo resuply
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && ((fields[i+1][j+1].getRodzajPola()==RodzajPola.ammo_resuply)
							  												  || (fields[i+1][j-1].getRodzajPola()==RodzajPola.ammo_resuply)
							  												  || (fields[i-1][j-1].getRodzajPola()==RodzajPola.ammo_resuply)
							  												  || (fields[i-1][j+1].getRodzajPola()==RodzajPola.ammo_resuply)
							  												  || (fields[i][j+1].getRodzajPola()==RodzajPola.ammo_resuply)
							  												  || (fields[i][j-1].getRodzajPola()==RodzajPola.ammo_resuply)
							  												  || (fields[i-1][j].getRodzajPola()==RodzajPola.ammo_resuply)
							  												  || (fields[i+1][j].getRodzajPola()==RodzajPola.ammo_resuply)))
	        		{
	        			ammoCounterBullet = 10;
	        			ammoCounterNuke = 5;
	        			ammoCounterGauss = 10;
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void EndGameBlackScreenCheck(){
		try{//zaczernienie planszy po smierci
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
	        			lifeCounter = 1;
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void Crosshair(){
		try{//wskaznik laserowy
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction==Direction.dirR)){
	        			try{
	        				for (int ii=0;ii<100;ii++)
	        		        	for (int jj=0;jj<100;jj++){
	        		        		if ((ii == i) && (jj > j) && (fields[ii][jj].getRodzajPola()==RodzajPola.empty)) 
	        		        			fields[ii][jj].setColor(Color.PINK);
	        		        	}
	        			} catch (Exception ee){}
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction==Direction.dirD)){
	        			try{
	        				for (int ii=0;ii<100;ii++)
	        		        	for (int jj=0;jj<100;jj++){
	        		        		if ((ii > i) && (jj == j) && (fields[ii][jj].getRodzajPola()==RodzajPola.empty)) 
	        		        			fields[ii][jj].setColor(Color.PINK);
	        		        	}
	        			} catch (Exception ee){}
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction==Direction.dirL)){
	        			try{
	        				for (int ii=99;ii>0;ii--)
	        		        	for (int jj=99;jj>0;jj--){
	        		        		if ((ii == i) && (jj < j) && (fields[ii][jj].getRodzajPola()==RodzajPola.empty)) 
	        		        			fields[ii][jj].setColor(Color.PINK);
	        		        	}
	        			} catch (Exception ee){}
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction==Direction.dirU)){
	        			try{
	        				for (int ii=99;ii>0;ii--)
	        		        	for (int jj=99;jj>0;jj--){
	        		        		if ((ii < i) && (jj == j) && (fields[ii][jj].getRodzajPola()==RodzajPola.empty)) 
	        		        			fields[ii][jj].setColor(Color.PINK);
	        		        	}
	        			} catch (Exception ee){}
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void MoveCruiser(){
		try{//krazownik dokola czolgu
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank)){
	        			if (cruiserPosCounter == 0){
	        				fields[i-3][j].setRodzajPola(RodzajPola.cruiser);
	        				fields[i-2][j-2].setRodzajPola(RodzajPola.empty);
	        			} else if (cruiserPosCounter == 1){
	        				fields[i-2][j+2].setRodzajPola(RodzajPola.cruiser);
	        				fields[i-3][j].setRodzajPola(RodzajPola.empty);
	        			} else if (cruiserPosCounter == 2){
	        				fields[i][j+3].setRodzajPola(RodzajPola.cruiser);
	        				fields[i-2][j+2].setRodzajPola(RodzajPola.empty);
	        			} else if (cruiserPosCounter == 3){
	        				fields[i+2][j+2].setRodzajPola(RodzajPola.cruiser);
	        				fields[i][j+3].setRodzajPola(RodzajPola.empty);
	        			} else if (cruiserPosCounter == 4){
	        				fields[i+3][j].setRodzajPola(RodzajPola.cruiser);
	        				fields[i+2][j+2].setRodzajPola(RodzajPola.empty);
	        			} else if (cruiserPosCounter == 5){
	        				fields[i+2][j-2].setRodzajPola(RodzajPola.cruiser);
	        				fields[i+3][j].setRodzajPola(RodzajPola.empty);
	        			} else if (cruiserPosCounter == 6){
	        				fields[i][j-3].setRodzajPola(RodzajPola.cruiser);
	        				fields[i+2][j-2].setRodzajPola(RodzajPola.empty);
	        			} else if (cruiserPosCounter == 7){
	        				fields[i-2][j-2].setRodzajPola(RodzajPola.cruiser);
	        				fields[i][j-3].setRodzajPola(RodzajPola.empty);
	        			}
	        		}
	        	}
			} catch (Exception e){}
		try{//cleaning cruiser
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((cruiserPosCounter == 0) && (fields[i][j].getRodzajPola()==RodzajPola.cruiser) && (fields[i+3][j].getRodzajPola()!=RodzajPola.tank)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        		if ((cruiserPosCounter == 1) && (fields[i][j].getRodzajPola()==RodzajPola.cruiser) && (fields[i+2][j-2].getRodzajPola()!=RodzajPola.tank)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        		if ((cruiserPosCounter == 2) && (fields[i][j].getRodzajPola()==RodzajPola.cruiser) && (fields[i][j-3].getRodzajPola()!=RodzajPola.tank)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        		if ((cruiserPosCounter == 3) && (fields[i][j].getRodzajPola()==RodzajPola.cruiser) && (fields[i-2][j-2].getRodzajPola()!=RodzajPola.tank)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        		if ((cruiserPosCounter == 4) && (fields[i][j].getRodzajPola()==RodzajPola.cruiser) && (fields[i-3][j].getRodzajPola()!=RodzajPola.tank)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        		if ((cruiserPosCounter == 5) && (fields[i][j].getRodzajPola()==RodzajPola.cruiser) && (fields[i-2][j+2].getRodzajPola()!=RodzajPola.tank)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        		if ((cruiserPosCounter == 6) && (fields[i][j].getRodzajPola()==RodzajPola.cruiser) && (fields[i][j+3].getRodzajPola()!=RodzajPola.tank)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        		if ((cruiserPosCounter == 7) && (fields[i][j].getRodzajPola()==RodzajPola.cruiser) && (fields[i+2][j+2].getRodzajPola()!=RodzajPola.tank)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
		
		try{//cruiser wybucha jak cos napotka
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.cruiser) && ((fields[i-1][j].getRodzajPola()!=RodzajPola.empty) || (fields[i][j-1].getRodzajPola()!=RodzajPola.empty) || (fields[i+1][j].getRodzajPola()!=RodzajPola.empty) || (fields[i][j+1].getRodzajPola()!=RodzajPola.empty))){
	        			fields[i][j].setRodzajPola(RodzajPola.to_explode);
	        		}
	        	}
			} catch (Exception e){}
		cruiserPosCounter++;
		if (cruiserPosCounter == 8)
			cruiserPosCounter = 0;
	}
	
	public void CruiserCleanup(){
		try{//czyscimy pozostalosci cruiserow z mapy jak ich nie uzywamy
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.cruiser) && (iChooseCruiser == false)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void ChangeTmpToMissiles(){
		try{//tmp_missile_l na missile_l LEFT
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tmp_missile_l)){
	        			fields[i][j].setRodzajPola(RodzajPola.missile_l);
	        		}
	        	}
			} catch (Exception e){}
		try{//tmp_missile_u na missile_u UP
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tmp_missile_u)){
	        			fields[i][j].setRodzajPola(RodzajPola.missile_u);
	        		}
	        	}
			} catch (Exception e){}
		try{//tmp_missile_r na missile_r RIGHT
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tmp_missile_r)){
	        			fields[i][j].setRodzajPola(RodzajPola.missile_r);
	        		}
	        	}
			} catch (Exception e){}
		try{//tmp_missile_d na missile_d DOWN
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tmp_missile_d)){
	        			fields[i][j].setRodzajPola(RodzajPola.missile_d);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void ChangeTmpToMissilesComp(){
		try{//tmp_missile_lc na missile_l LEFT
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tmp_missile_lc)){
	        			fields[i][j].setRodzajPola(RodzajPola.missile_lc);
	        		}
	        	}
			} catch (Exception e){}
		try{//tmp_missile_uc na missile_u UP
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tmp_missile_uc)){
	        			fields[i][j].setRodzajPola(RodzajPola.missile_uc);
	        		}
	        	}
			} catch (Exception e){}
		try{//tmp_missile_rc na missile_r RIGHT
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tmp_missile_rc)){
	        			fields[i][j].setRodzajPola(RodzajPola.missile_rc);
	        		}
	        	}
			} catch (Exception e){}
		try{//tmp_missile_dc na missile_d DOWN
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tmp_missile_dc)){
	        			fields[i][j].setRodzajPola(RodzajPola.missile_dc);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void MoveMissile(){
		try{//przesuniecie missile_l <<
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_l) && (fields[i][j-1].getRodzajPola()==RodzajPola.empty)){
        			fields[i][j-1].setRodzajPola(RodzajPola.tmp_missile_l);
        			fields[i][j].setRodzajPola(RodzajPola.empty);
        		}
        	}
		} catch (Exception e){}
		try{//przesuniecie missile_u ^
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_u) && (fields[i-1][j].getRodzajPola()==RodzajPola.empty)){
	        			fields[i-1][j].setRodzajPola(RodzajPola.tmp_missile_u);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
		try{//przesuniecie missile_r >>
		for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_r) && (fields[i][j+1].getRodzajPola()==RodzajPola.empty)){
        			fields[i][j+1].setRodzajPola(RodzajPola.tmp_missile_r);
        			fields[i][j].setRodzajPola(RodzajPola.empty);
        		}
        	}
		} catch (Exception e){}
		try{//przesuniecie missile_d V
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_d) && (fields[i+1][j].getRodzajPola()==RodzajPola.empty)){
	        			fields[i+1][j].setRodzajPola(RodzajPola.tmp_missile_d);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void MoveMissileInstantly(){//pocisk uderza natychmiastowo
		try{//przesuniecie missile_l <<
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_l) && (fields[i][j-1].getRodzajPola()==RodzajPola.empty)){
        			fields[i][j-1].setRodzajPola(RodzajPola.missile_l);
        			fields[i][j].setRodzajPola(RodzajPola.empty);
        		}
        	}
		} catch (Exception e){}
		try{//przesuniecie missile_u ^
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_u) && (fields[i-1][j].getRodzajPola()==RodzajPola.empty)){
	        			fields[i-1][j].setRodzajPola(RodzajPola.missile_u);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
		try{//przesuniecie missile_r >>
		for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_r) && (fields[i][j+1].getRodzajPola()==RodzajPola.empty)){
        			fields[i][j+1].setRodzajPola(RodzajPola.missile_r);
        			fields[i][j].setRodzajPola(RodzajPola.empty);
        		}
        	}
		} catch (Exception e){}
		try{//przesuniecie missile_d V
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_d) && (fields[i+1][j].getRodzajPola()==RodzajPola.empty)){
	        			fields[i+1][j].setRodzajPola(RodzajPola.missile_d);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void MoveMissileGauss(){
		try{//przesuniecie missile_l << IF GAUSS
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_l) && (fields[i][j-1].getRodzajPola()==RodzajPola.empty)){
        			fields[i][j-1].setRodzajPola(RodzajPola.tmp_missile_l);//fast projectile
        			fields[i][j-2].setRodzajPola(RodzajPola.tmp_missile_l);
        			fields[i][j-3].setRodzajPola(RodzajPola.tmp_missile_l);
        			fields[i][j-4].setRodzajPola(RodzajPola.tmp_missile_l);
        			fields[i][j-5].setRodzajPola(RodzajPola.tmp_missile_l);
        			fields[i][j].setRodzajPola(RodzajPola.empty);
        		}
        	}
		} catch (Exception e){}
		try{//przesuniecie missile_u ^ IF GAUSS
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_u) && (fields[i-1][j].getRodzajPola()==RodzajPola.empty)){
	        			fields[i-1][j].setRodzajPola(RodzajPola.tmp_missile_u);
	        			fields[i-2][j].setRodzajPola(RodzajPola.tmp_missile_u);//bez tego for torpeda
	        			fields[i-3][j].setRodzajPola(RodzajPola.tmp_missile_u);//bez tego for torpeda
	        			fields[i-4][j].setRodzajPola(RodzajPola.tmp_missile_u);
	        			fields[i-5][j].setRodzajPola(RodzajPola.tmp_missile_u);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
		try{//przesuniecie missile_r >> IF GAUSS
		for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_r) && (fields[i][j+1].getRodzajPola()==RodzajPola.empty)){
        			fields[i][j+1].setRodzajPola(RodzajPola.tmp_missile_r);
        			fields[i][j+2].setRodzajPola(RodzajPola.tmp_missile_r);
        			fields[i][j+3].setRodzajPola(RodzajPola.tmp_missile_r);
        			fields[i][j+4].setRodzajPola(RodzajPola.tmp_missile_r);
        			fields[i][j+5].setRodzajPola(RodzajPola.tmp_missile_r);
        			fields[i][j].setRodzajPola(RodzajPola.empty);
        		}
        	}
		} catch (Exception e){}
		try{//przesuniecie missile_d V IF GAUSS
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_d) && (fields[i+1][j].getRodzajPola()==RodzajPola.empty)){
	        			fields[i+1][j].setRodzajPola(RodzajPola.tmp_missile_d);
	        			fields[i+2][j].setRodzajPola(RodzajPola.tmp_missile_d);
	        			fields[i+3][j].setRodzajPola(RodzajPola.tmp_missile_d);
	        			fields[i+4][j].setRodzajPola(RodzajPola.tmp_missile_d);
	        			fields[i+5][j].setRodzajPola(RodzajPola.tmp_missile_d);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void MoveCompMissile(){
		try{//przesuniecie missile_lc <<
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_lc) && (fields[i][j-1].getRodzajPola()==RodzajPola.empty)){
        			fields[i][j-1].setRodzajPola(RodzajPola.tmp_missile_lc);
        			fields[i][j].setRodzajPola(RodzajPola.empty);
        		}
        	}
		} catch (Exception e){}
		try{//przesuniecie missile_uc ^
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_uc) && (fields[i-1][j].getRodzajPola()==RodzajPola.empty)){
	        			fields[i-1][j].setRodzajPola(RodzajPola.tmp_missile_uc);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
		try{//przesuniecie missile_rc >>
		for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_rc) && (fields[i][j+1].getRodzajPola()==RodzajPola.empty)){
        			fields[i][j+1].setRodzajPola(RodzajPola.tmp_missile_rc);
        			fields[i][j].setRodzajPola(RodzajPola.empty);
        		}
        	}
		} catch (Exception e){}
		try{//przesuniecie missile_dc V
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_dc) && (fields[i+1][j].getRodzajPola()==RodzajPola.empty)){
	        			fields[i+1][j].setRodzajPola(RodzajPola.tmp_missile_dc);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void ExplosionInit(){
		try{//inicjacja eksplozji W LEWO
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_lc) && (fields[i][j-1].getRodzajPola()!=RodzajPola.empty) && (typPociskuKomputera == TypeOfProjectile.bullet))
	        			fields[i][j-1].setRodzajPola(RodzajPola.to_explode);//dla komputera
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_l) && (fields[i][j-1].getRodzajPola()!=RodzajPola.empty) && ((typPociskuGracza == TypeOfProjectile.bullet) || (typPociskuGracza == TypeOfProjectile.gauss))){
	        			fields[i][j-1].setRodzajPola(RodzajPola.to_explode);      			
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.missile_l) && (fields[i][j-1].getRodzajPola()!=RodzajPola.empty) && (typPociskuGracza == TypeOfProjectile.nuke)){
	        			fields[i][j].setRodzajPola(RodzajPola.to_explode);  
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j+ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j+ran].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j+ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j+ran].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i][j+ran].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i][j-ran].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j].setRodzajPola(RodzajPola.to_explode);
	        		}
	        	}
			} catch (Exception e){}
			try{//inicjacja eksplozji W GORE
				for (int i=0;i<100;i++)
		        	for (int j=0;j<100;j++){
		        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_uc) && (fields[i-1][j].getRodzajPola()!=RodzajPola.empty) && (typPociskuKomputera == TypeOfProjectile.bullet))
		        			fields[i-1][j].setRodzajPola(RodzajPola.to_explode);//dla komputera
		        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_u) && (fields[i-1][j].getRodzajPola()!=RodzajPola.empty) && ((typPociskuGracza == TypeOfProjectile.bullet) || (typPociskuGracza == TypeOfProjectile.gauss))){
		        			fields[i-1][j].setRodzajPola(RodzajPola.to_explode);      			
		        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.missile_u) && (fields[i-1][j].getRodzajPola()!=RodzajPola.empty) && (typPociskuGracza == TypeOfProjectile.nuke)){
		        			fields[i][j].setRodzajPola(RodzajPola.to_explode);  
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j+ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j+ran].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j+ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j+ran].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i][j+ran].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i][j-ran].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j].setRodzajPola(RodzajPola.to_explode);
		        		}
		        	}
				} catch (Exception e){}
			try{//inicjacja eksplozji W PRAWO
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_rc) && (fields[i][j+1].getRodzajPola()!=RodzajPola.empty) && (typPociskuKomputera == TypeOfProjectile.bullet))
	        			fields[i][j+1].setRodzajPola(RodzajPola.to_explode);//dla komputera
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_r) && (fields[i][j+1].getRodzajPola()!=RodzajPola.empty) && ((typPociskuGracza == TypeOfProjectile.bullet) || (typPociskuGracza == TypeOfProjectile.gauss))){
	        			fields[i][j+1].setRodzajPola(RodzajPola.to_explode);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.missile_r) && (fields[i][j+1].getRodzajPola()!=RodzajPola.empty) && (typPociskuGracza == TypeOfProjectile.nuke)){
	        			fields[i][j].setRodzajPola(RodzajPola.to_explode);  
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j+ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j+ran].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j+ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j+ran].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i][j+ran].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i][j-ran].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i+ran][j].setRodzajPola(RodzajPola.to_explode);
	        				ran = ThreadLocalRandom.current().nextInt(2, 8);
	        			fields[i-ran][j].setRodzajPola(RodzajPola.to_explode);
	        		}
	        	}
			} catch (Exception e){}
			try{//inicjacja eksplozji W DOL
				for (int i=0;i<100;i++)
		        	for (int j=0;j<100;j++){
		        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_dc) && (fields[i+1][j].getRodzajPola()!=RodzajPola.empty) && (typPociskuKomputera == TypeOfProjectile.bullet))
		        			fields[i+1][j].setRodzajPola(RodzajPola.to_explode);//dla komputera
		        		if ((fields[i][j].getRodzajPola()==RodzajPola.missile_d) && (fields[i+1][j].getRodzajPola()!=RodzajPola.empty) && ((typPociskuGracza == TypeOfProjectile.bullet) || (typPociskuGracza == TypeOfProjectile.gauss))){
		        			fields[i+1][j].setRodzajPola(RodzajPola.to_explode);
		        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.missile_d) && (fields[i+1][j].getRodzajPola()!=RodzajPola.empty) && (typPociskuGracza == TypeOfProjectile.nuke)){
		        			fields[i][j].setRodzajPola(RodzajPola.to_explode);  
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j+ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j+ran].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j+ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j-ran].setRodzajPola(RodzajPola.to_explode); 
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j+ran].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i][j+ran].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i][j-ran].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i+ran][j].setRodzajPola(RodzajPola.to_explode);
		        				ran = ThreadLocalRandom.current().nextInt(2, 8);
		        			fields[i-ran][j].setRodzajPola(RodzajPola.to_explode);
		        		}
		        	}
				} catch (Exception e){}
	}
	
	public void ExplosionCleanup(){
		try{//cleanup po ekplozji
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.explosion)){
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){			
			}
	}
	
	public void DrawUnarmoredTank(){
		try{//usuwaj skrzydla czolgu, ktore zostawaly za czolgiem
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank_wings) && (fields[i+1][j+1].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i+1][j-1].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i-1][j-1].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i-1][j+1].getRodzajPola()!=RodzajPola.tank))
	        		{
	        			fields[i][j].setRodzajPola(RodzajPola.empty);													
	        		}
	        	} 
		}catch (Exception e){}
		try{//maluj skrzydla czolgu
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction == Direction.dirU)){
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction == Direction.dirD)){
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction == Direction.dirR)){
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction == Direction.dirL)){
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void DrawArmoredTank(){
		try{//usuwaj reszte czolgu, ktora za nim zostanie
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank_wings) && (fields[i+1][j+1].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i+1][j-1].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i-1][j-1].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i-1][j+1].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i][j+1].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i][j-1].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i-1][j].getRodzajPola()!=RodzajPola.tank)
	        																  && (fields[i+1][j].getRodzajPola()!=RodzajPola.tank))
	        		{
	        			fields[i][j].setRodzajPola(RodzajPola.empty);													
	        		}
	        	} 
		}catch (Exception e){}

		try{//maluj reszte czolgu opancerzonego
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction == Direction.dirD)){
	        			fields[i-1][j].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction == Direction.dirU)){
	        			fields[i+1][j].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction == Direction.dirR)){
	        			fields[i][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.tank) && (direction == Direction.dirL)){
	        			fields[i][j+1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.tank_wings);
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.tank_wings);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void DrawHelicopter(){
		try{//usuwaj reszte helikoptera, ktora zostawala za helikopterem
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.heli_wings) && (fields[i+1][j+1].getRodzajPola()!=RodzajPola.heli)
	        																  && (fields[i+1][j-1].getRodzajPola()!=RodzajPola.heli)
	        																  && (fields[i-1][j-1].getRodzajPola()!=RodzajPola.heli)
	        																  && (fields[i-1][j+1].getRodzajPola()!=RodzajPola.heli)
	        																  && (fields[i][j+1].getRodzajPola()!=RodzajPola.heli)
	        																  && (fields[i][j-1].getRodzajPola()!=RodzajPola.heli)
	        																  && (fields[i-1][j].getRodzajPola()!=RodzajPola.heli)
	        																  && (fields[i+1][j].getRodzajPola()!=RodzajPola.heli))
	        		{
	        			fields[i][j].setRodzajPola(RodzajPola.empty);													
	        		}
	        	} 
		}catch (Exception e){}

		try{//maluj reszte helikoptera
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.heli) && (heliDirection == Direction.dirD)){
	        			fields[i-1][j].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.heli_wings);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.heli) && (heliDirection == Direction.dirU)){
	        			fields[i+1][j].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.heli_wings);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.heli) && (heliDirection == Direction.dirR)){
	        			fields[i][j-1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.heli_wings);
	        		} else if ((fields[i][j].getRodzajPola()==RodzajPola.heli) && (heliDirection == Direction.dirL)){
	        			fields[i][j+1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i-1][j-1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i-1][j+1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i+1][j-1].setRodzajPola(RodzajPola.heli_wings);
	        			fields[i+1][j+1].setRodzajPola(RodzajPola.heli_wings);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void DrawVehicles(){
		if (typeOfTank == Player.unarmoured_tank){
			DrawUnarmoredTank();
		} else if (typeOfTank == Player.armoured_tank){
			DrawArmoredTank();
		}
		DrawHelicopter();
	}
	
	public void TrumpWall(){
		try{//Trump's Wall
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if (((i==0 || j == 0) || (i == 99 || j == 99)) || ((i==1 || j == 1) || (i == 98 || j == 98))){
	            		fields[i][j].setRodzajPola(RodzajPola.unpassable);
	        		}
	        	}
			} catch (Exception e){			
			}
	}
	
	public void EndGameBlackScreenExecute(){
		if (lifeCounter == 0)//jesli DEAD to ustawiamy na czarno
			try{
				for (int i=0;i<500;i++){
		        		ran = ThreadLocalRandom.current().nextInt(1, 99);
		        		ran2 = ThreadLocalRandom.current().nextInt(1, 99);
		        		fields[ran][ran2].setRodzajPola(RodzajPola.unpassable);
				}
			} catch (Exception e){}
	}
	
	public void ShootingTurrets(){
		try{//shooting turret_d
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.turret_d)){
	        			ran = ThreadLocalRandom.current().nextInt(0, 99);
	        			if (ran>=96)
	        			fields[i+2][j].setRodzajPola(RodzajPola.missile_dc);
	        		}
	        	}
			} catch (Exception e){}
		try{//shooting turret_u
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.turret_u)){
	        			ran = ThreadLocalRandom.current().nextInt(0, 99);
	        			if (ran>=96)
	        			fields[i-2][j].setRodzajPola(RodzajPola.missile_uc);
	        		}
	        	}
			} catch (Exception e){}
		try{//shooting turret_l
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.turret_l)){
	        			ran = ThreadLocalRandom.current().nextInt(0, 99);
	        			if (ran>=96)
	        			fields[i][j-2].setRodzajPola(RodzajPola.missile_lc);
	        		}
	        	}
			} catch (Exception e){}
		try{//shooting turret_r
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((fields[i][j].getRodzajPola()==RodzajPola.turret_r)){
	        			ran = ThreadLocalRandom.current().nextInt(0, 99);
	        			if (ran>=96)
	        			fields[i][j+2].setRodzajPola(RodzajPola.missile_rc);
	        		}
	        	}
			} catch (Exception e){}
	}
	
	public void MovingBoss(){
		try{//moving Boss
			for (int i=0;i<100;i++)//sprawdzamy czy boss nie wyszedl poza swoje ramy
				for (int j=0;j<100;j++){
					if ((fields[i][j].getRodzajPola()==RodzajPola.boss) && ((j>30) || (j<8)))
						for (int ii=0;ii<100;ii++)//jesli wyszedl to kasujemy go i rysujemy nowego
							for (int jj=0;jj<100;jj++){
								if ((fields[ii][jj].getRodzajPola()==RodzajPola.boss) || (fields[ii][jj].getRodzajPola()==RodzajPola.turret_d)){
									fields[ii][jj].setRodzajPola(RodzajPola.empty);
								}
								Boss();
								speedOfBoss = 93;
							}
				}
			ran = ThreadLocalRandom.current().nextInt(0, 99);
			if (ran>speedOfBoss){
				for (int i=0;i<100;i++)
					for (int j=0;j<100;j++){
						if (fields[i][j].getRodzajPola()==RodzajPola.boss){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j-1].setRodzajPola(RodzajPola.boss);
						}
					}
				for (int i=0;i<100;i++)
					for (int j=0;j<100;j++){
						if (fields[i][j].getRodzajPola()==RodzajPola.turret_d){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j-1].setRodzajPola(RodzajPola.turret_d);
						}
					}
			}
			ran = ThreadLocalRandom.current().nextInt(0, 99);
			if (ran>speedOfBoss){
				for (int i=99;i>0;i--)
					for (int j=99;j>0;j--){
						if (fields[i][j].getRodzajPola()==RodzajPola.boss){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j+1].setRodzajPola(RodzajPola.boss);
						}
					}
				for (int i=99;i>0;i--)
					for (int j=99;j>0;j--){
						if (fields[i][j].getRodzajPola()==RodzajPola.turret_d){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j+1].setRodzajPola(RodzajPola.turret_d);
						}
					}
			}
		} catch (Exception e){}
	}
	
	public void MovingBoss2(){
		try{//moving Boss
			for (int i=0;i<100;i++)//sprawdzamy czy boss nie wyszedl poza swoje ramy
				for (int j=0;j<100;j++){
					if ((fields[i][j].getRodzajPola()==RodzajPola.boss2) && ((j>67) || (j<50)))
						for (int ii=0;ii<100;ii++)//jesli wyszedl to kasujemy go i rysujemy nowego
							for (int jj=0;jj<100;jj++){
								if ((fields[ii][jj].getRodzajPola()==RodzajPola.boss2) || (fields[ii][jj].getRodzajPola()==RodzajPola.turret_d)){
									fields[ii][jj].setRodzajPola(RodzajPola.empty);
								}
								Boss2();
								speedOfBoss = 93;
							}
				}
			ran = ThreadLocalRandom.current().nextInt(0, 99);
			if (ran>speedOfBoss){
				for (int i=0;i<100;i++)
					for (int j=0;j<100;j++){
						if (fields[i][j].getRodzajPola()==RodzajPola.boss2){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j-1].setRodzajPola(RodzajPola.boss2);
						}
					}
				for (int i=0;i<100;i++)
					for (int j=0;j<100;j++){
						if (fields[i][j].getRodzajPola()==RodzajPola.turret_d){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j-1].setRodzajPola(RodzajPola.turret_d);
						}
					}
			}
			ran = ThreadLocalRandom.current().nextInt(0, 99);
			if (ran>speedOfBoss){
				for (int i=99;i>0;i--)
					for (int j=99;j>0;j--){
						if (fields[i][j].getRodzajPola()==RodzajPola.boss2){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j+1].setRodzajPola(RodzajPola.boss2);
						}
					}
				for (int i=99;i>0;i--)
					for (int j=99;j>0;j--){
						if (fields[i][j].getRodzajPola()==RodzajPola.turret_d){
							fields[i][j].setRodzajPola(RodzajPola.empty);
							fields[i][j+1].setRodzajPola(RodzajPola.turret_d);
						}
					}
			}
		} catch (Exception e){}
	}
	
	public void DisplayAmmo(){
		
	}
	
	public void Move(){
		if (ueCounter == 3){//wybuch UE; gracz zwieksza licznik do 6 co powoduje 3 stopniowa eksplozje
			UltimateExplosion();
		} else if (ueCounter == 6){
			UltimateExplosion();
		} else if (ueCounter == 9){
			UltimateExplosion();
		}
		if (ueCounter >= 0)
			ueCounter--;
		CollectPowerups();
		DrawVehicles();
		lifeCounter = 0;
		EndGameBlackScreenCheck();
		ChangeTmpToMissiles();
		ChangeTmpToMissilesComp();
		Maluj();
		
		if (typPociskuGracza == TypeOfProjectile.bullet){
			MoveMissileInstantly();
		} else if (typPociskuGracza == TypeOfProjectile.gauss){
			MoveMissileGauss();
		} else if (typPociskuGracza == TypeOfProjectile.nuke){
			MoveMissile();
		}
		
		if (iChooseCruiser == true)
		MoveCruiser();
		CruiserCleanup();
		
		
		MoveCompMissile();
		ExplosionCleanup();
		ExplosionInit();
		Explosion();
		TrumpWall();
		EndGameBlackScreenExecute();
		ShootingTurrets();
		try{//you can't smash into something
			for (int i=99;i>0;i--)
	        	for (int j=99;j>0;j--){
	        		if ((direction == Direction.dirU) && (fields[i][j].getRodzajPola()==RodzajPola.tank) && (fields[i-1][j].getRodzajPola()!=RodzajPola.empty) && (fields[i-1][j].getRodzajPola()!=RodzajPola.cruiser_powerup) && (fields[i-1][j].getRodzajPola()!=RodzajPola.tmp_missile_dc)){
	        			fields[i+1][j].setRodzajPola(RodzajPola.tank);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			for (int i=99;i>0;i--)
	        	for (int j=99;j>0;j--){
	        		if ((direction == Direction.dirL) && (fields[i][j].getRodzajPola()==RodzajPola.tank) && (fields[i][j-1].getRodzajPola()!=RodzajPola.empty) && (fields[i][j-1].getRodzajPola()!=RodzajPola.cruiser_powerup) && (fields[i][j-1].getRodzajPola()!=RodzajPola.tmp_missile_rc)){
	        			fields[i][j+1].setRodzajPola(RodzajPola.tank);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((direction == Direction.dirR) && (fields[i][j].getRodzajPola()==RodzajPola.tank) && (fields[i][j+1].getRodzajPola()!=RodzajPola.empty) && (fields[i][j+1].getRodzajPola()!=RodzajPola.cruiser_powerup) && (fields[i][j+1].getRodzajPola()!=RodzajPola.tmp_missile_lc)){
	        			fields[i][j-1].setRodzajPola(RodzajPola.tank);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			for (int i=0;i<100;i++)
	        	for (int j=0;j<100;j++){
	        		if ((direction == Direction.dirD) && (fields[i][j].getRodzajPola()==RodzajPola.tank) && (fields[i+1][j].getRodzajPola()!=RodzajPola.empty) && (fields[i+1][j].getRodzajPola()!=RodzajPola.cruiser_powerup) && (fields[i+1][j].getRodzajPola()!=RodzajPola.tmp_missile_uc)){
	        			fields[i-1][j].setRodzajPola(RodzajPola.tank);
	        			fields[i][j].setRodzajPola(RodzajPola.empty);
	        		}
	        	}
			} catch (Exception e){}
		try{//Walls inside the map
			for (int i=0;i<100;i++)
				for (int j=0;j<100;j++){
					if((j>35 && j<38) && (i>40 || i<25))
						fields[i][j].setRodzajPola(RodzajPola.unpassable);
				}
		} catch (Exception e){}
		heliLive = false;
		try{//check if helicopter still live
			for (int i=0;i<100;i++)
				for (int j=0;j<100;j++){
					if(fields[i][j].getRodzajPola()==RodzajPola.heli)
						heliLive = true;
				}
		} catch (Exception e){}
		boss2Counter = 0;
		try{//check if boss2 still live
			for (int i=0;i<100;i++)
				for (int j=0;j<100;j++){
					if(fields[i][j].getRodzajPola()==RodzajPola.boss2)
						boss2Counter += 1;
				}
		} catch (Exception e){}
		if (heliLive == false){
			lifeCounter = 0;
			EndGameBlackScreenExecute();
		}
		if (baseVisited == true)
		MovingBoss2();
		if (baseVisited == false)
		MovingBoss();
		for (int i=0;i<100;i++)//sprawdz ile zostalo boss i zwieksz poziom trudnosci
			for (int j=0;j<100;j++){
				if (fields[i][j].getRodzajPola()==RodzajPola.boss){
					bossCounter += 1;
				}
			}
		if ((boss2Counter == 0) && (baseVisited == true))
			heliCtBg += 20;
		if ((bossCounter <15) && (bossCounter > 0))
			speedOfBoss = 30;
		if ((bossCounter == 0) && (baseVisited == false))//jak padnie boss1 to przyspiesza helikopter
			heliCtBg += 10;
		
		bossCounter = 0;
		heliCtBg++;
		
		if (heliCtBg >= 20){//wykonaj ruch helikoptera co tyle punktow turowych
			heliCtLw ++;
			MoveHeli();
			heliCtBg = 0;
		}
		for (int i=0;i<100;i++)//sprawdz ile zostalo HP bazie
			for (int j=0;j<100;j++){
				if (fields[i][j].getRodzajPola()==RodzajPola.base_health){
					baseLifeCounter += 1;
				}
			}
		if (baseLifeCounter <7){//game over jak baza zniszczona
			lifeCounter = 0;
			EndGameBlackScreenExecute();
		}
		baseLifeCounter = 0;
		
		if (crosshair == true)
		Crosshair();
	}//endof Move();
	
	public void MoveHeli(){//funkcja kierujaca helikopter po planszy
		if (heliCtLw < 50){
			MoveHeliL();
		}
		if ((heliCtLw > 50) && (heliCtLw <100)){
			MoveHeliU();
		}
		if ((heliCtLw > 100) && (heliCtLw <115)){
			MoveHeliL();
		}
		if ((heliCtLw > 115) && (heliCtLw <175)){
			MoveHeliD();
		}
		if ((heliCtLw > 175) && (heliCtLw <185)){
			MoveHeliL();
			fields[85][20].setRodzajPola(RodzajPola.ammo_resuply);
		}
		if ((heliCtLw > 215) && (heliCtLw <225)){
			Boss2();
			BaseHealth();
			baseVisited = true;
		}
		if ((heliCtLw > 225) && (heliCtLw <235)){
			MoveHeliR();
		}
		if ((heliCtLw > 235) && (heliCtLw <295)){
			MoveHeliU();
		}
		if ((heliCtLw > 295) && (heliCtLw <310)){
			MoveHeliR();
		}
		if ((heliCtLw > 310) && (heliCtLw <335)){
			MoveHeliU();
		}
		if ((heliCtLw > 335) && (heliCtLw <385)){
			MoveHeliR();
		}

	}
	
	public void MoveHeliL(){
		try{
			for (int i=0;i<100;i++)
				for (int j=0;j<100;j++){
					if ((fields[i][j].getRodzajPola()==RodzajPola.heli)){
						fields[i][j].setRodzajPola(RodzajPola.empty);
						fields[i][j-1].setRodzajPola(RodzajPola.heli);
					}
				}
			} catch (Exception e){}
		heliDirection = Direction.dirL;
	}
	public void MoveHeliR(){
		try{
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
					if ((fields[i][j].getRodzajPola()==RodzajPola.heli)){
						fields[i][j].setRodzajPola(RodzajPola.empty);
						fields[i][j+1].setRodzajPola(RodzajPola.heli);
					}
				}
			} catch (Exception e){}
		heliDirection = Direction.dirR;
	}
	public void MoveHeliU(){
		try{
			for (int i=0;i<100;i++)
				for (int j=0;j<100;j++){
					if ((fields[i][j].getRodzajPola()==RodzajPola.heli)){
						fields[i][j].setRodzajPola(RodzajPola.empty);
						fields[i-1][j].setRodzajPola(RodzajPola.heli);
					}
				}
			} catch (Exception e){}
		heliDirection = Direction.dirU;
	}
	public void MoveHeliD(){
		try{
			for (int i=99;i>=0;i--)
				for (int j=99;j>=0;j--){
					if ((fields[i][j].getRodzajPola()==RodzajPola.heli)){
						fields[i][j].setRodzajPola(RodzajPola.empty);
						fields[i+1][j].setRodzajPola(RodzajPola.heli);
					}
				}
			} catch (Exception e){}
		heliDirection = Direction.dirD;
	}
	
	public void AmmoCounterDisplay(){
		try{
			for (int i=0; i<=ammoCounterBullet; i++){//BULLETY
				ammoTableBullet[i].setColor(Color.BLACK);
			}
		} catch (Exception e){}
		try{
			for (int i=ammoCounterBullet; i<=20; i++){
				ammoTableBullet[i].setColor(Color.WHITE);
			}
		} catch (Exception e){}
		try{
			for (int i=0; i<=ammoCounterNuke; i++){//NUKE
				ammoTableNuke[i].setColor(Color.BLACK);
			}
		} catch (Exception e){}
		try{
			for (int i=ammoCounterNuke; i<=20; i++){
				ammoTableNuke[i].setColor(Color.WHITE);
			}
		} catch (Exception e){}
		try{
			for (int i=0; i<=ammoCounterGauss; i++){//GAUSS
				ammoTableGauss[i].setColor(Color.BLACK);
			}
		} catch (Exception e){}
		try{
			for (int i=ammoCounterGauss; i<=20; i++){
				ammoTableGauss[i].setColor(Color.WHITE);
			}
		} catch (Exception e){}
	}

	
	public void TriggerFields(){//ustaw wszystko na planszy
		Walls();
		Base();
		CastleRight();
		Czolg();
		MapElements();
		Turrets();
		Boss();
		Helicopter();
		BaseHealth();
	}
	
	public void Maluj(){//zamiana typu pola na kolor
		switch(typPociskuGracza){
		case bullet:
			projectiles[0].setColor(Color.YELLOW);
			projectiles[1].setColor(Color.RED);
			projectiles[2].setColor(Color.RED);
			break;
		case nuke:
			projectiles[0].setColor(Color.RED);
			projectiles[1].setColor(Color.YELLOW);
			projectiles[2].setColor(Color.RED);
			break;
		case gauss:
			projectiles[0].setColor(Color.RED);
			projectiles[1].setColor(Color.RED);
			projectiles[2].setColor(Color.YELLOW);
			break;
		default:
			break;
		}
		for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		switch(fields[i][j].getRodzajPola()){
        		case empty:
        			fields[i][j].setColor(puste);
        			break;
        		case missile_l:
        			fields[i][j].setColor(pocisk);
        			break;
        		case missile_r:
        			fields[i][j].setColor(pocisk);
        			break;
        		case missile_u:
        			fields[i][j].setColor(pocisk);
        			break;
        		case missile_d:
        			fields[i][j].setColor(pocisk);
        			break;
        		case missile_lc:
        			fields[i][j].setColor(pocisk);
        			break;
        		case missile_rc:
        			fields[i][j].setColor(pocisk);
        			break;
        		case missile_uc:
        			fields[i][j].setColor(pocisk);
        			break;
        		case missile_dc:
        			fields[i][j].setColor(pocisk);
        			break;
        		case unpassable:
        			fields[i][j].setColor(mur);
        			break;
        		case building:
        			fields[i][j].setColor(zabudowanie);
        			break;
        		case to_explode:
        			fields[i][j].setColor(msc_eksplozji);
        			break;
        		case explosion:
        			fields[i][j].setColor(eksplozja);
        			break;
        		case tank:
        			fields[i][j].setColor(czolg);
        			break;
        		case tank_wings:
        			fields[i][j].setColor(czolg_skrzydlo);
        			break;
        		case heli:
        			fields[i][j].setColor(czolg);
        			break;
        		case heli_wings:
        			fields[i][j].setColor(czolg_skrzydlo);
        			break;
        		case cruiser_powerup:
        			fields[i][j].setColor(Color.ORANGE);
        			break;
				case turret_d:
					fields[i][j].setColor(Color.CYAN);
					break;
				case turret_u:
					fields[i][j].setColor(Color.CYAN);
					break;
				case turret_l:
					fields[i][j].setColor(Color.CYAN);
					break;
				case turret_r:
					fields[i][j].setColor(Color.CYAN);
					break;
				case boss:
					fields[i][j].setColor(Color.LIGHT_GRAY);
					break;
				case boss2:
					fields[i][j].setColor(Color.DARK_GRAY);
					break;
				case base_health:
					fields[i][j].setColor(Color.MAGENTA);
					break;
				case cruiser:
					fields[i][j].setColor(Color.YELLOW);
					break;
				case ammo_resuply:
					fields[i][j].setColor(Color.DARK_GRAY);
					break;
				default:
					break;
   	        		}
        	}//end of for
		AmmoCounterDisplay();
			
		repaint();
	}
	
	public void Gra(){
		Move();

	}
	
	public void Base(){
		for (int i=0;i<100;i++)//boss wall
        	for (int j=0;j<100;j++){
        		if (((i > 20) && (i < 30)) && ((j > 3) && (j < 34))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
		for (int i=0;i<100;i++)//okopy 1
        	for (int j=0;j<100;j++){
        		if (((i > 40) && (i < 45)) && ((j > 3) && (j < 7) || (j > 10) && (j < 14) || (j > 17) && (j < 21) || (j > 24) && (j < 28))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
		for (int i=0;i<100;i++)//okopy 2
        	for (int j=0;j<100;j++){
        		if (((i > 47) && (i < 51)) && ((j > 7) && (j < 10) || (j > 14) && (j < 17) || (j > 21) && (j < 24))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
		for (int i=0;i<100;i++)//first wall
        	for (int j=0;j<100;j++){
        		if (((i > 50) && (i < 55)) && ((j > 3) && (j < 29))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
		for (int i=0;i<100;i++)//second wall
        	for (int j=0;j<100;j++){
        		if (((i > 60) && (i < 65)) && ((j > 3) && (j < 29))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
		for (int i=0;i<100;i++)//third wall
        	for (int j=0;j<100;j++){
        		if (((i > 70) && (i < 75)) && ((j > 3) && (j < 29))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
		for (int i=0;i<100;i++)//side walls
        	for (int j=0;j<100;j++){
        		if (((i > 75) && (i < 98)) && ((j > 3) && (j < 8))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
		//turrets
		fields[40][5].setRodzajPola(RodzajPola.turret_u);
		fields[40][12].setRodzajPola(RodzajPola.turret_u);
		fields[40][19].setRodzajPola(RodzajPola.turret_u);
		fields[40][26].setRodzajPola(RodzajPola.turret_u);
	}
	public void CastleRight(){
		for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		if (((j > 50) && (j < 98)) && ((i > 20) && (i < 30))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
		for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		if (((j > 50) && (j < 80)) && ((i > 29) && (i < 60))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
		for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		if (((j > 50) && (j < 98)) && ((i > 59) && (i < 70))){
        			fields[i][j].setRodzajPola(RodzajPola.building);
        		}
        	}
	}
	
	public void Walls(){
		for (int i=0;i<100;i++)
        	for (int j=0;j<100;j++){
        		if ((i==0 || j == 0) || (i == 99 || j == 99)){
            		fields[i][j].setRodzajPola(RodzajPola.unpassable);
            	} else  {
            		fields[i][j].setRodzajPola(RodzajPola.empty);
            	}
        	}

	}
	public void Czolg(){
		fields[85][80].setRodzajPola(RodzajPola.tank);

	}
	
	public void MapElements(){
		fields[95][95].setRodzajPola(RodzajPola.cruiser_powerup);
	}
	
	public void Turrets(){//left map turrets
		fields[38][97].setRodzajPola(RodzajPola.turret_l);//wjazd do bazy
		fields[40][97].setRodzajPola(RodzajPola.turret_l);
		fields[37][37].setRodzajPola(RodzajPola.turret_r);//prawa sciana
		fields[39][37].setRodzajPola(RodzajPola.turret_r);
		fields[96][47].setRodzajPola(RodzajPola.turret_u);//zaraz na starcie
		fields[95][48].setRodzajPola(RodzajPola.turret_u);
		fields[95][49].setRodzajPola(RodzajPola.turret_u);
		fields[96][50].setRodzajPola(RodzajPola.turret_u);
	}
	
	public void Boss(){
		fields[5][18].setRodzajPola(RodzajPola.boss);
		fields[5][19].setRodzajPola(RodzajPola.boss);
		fields[5][20].setRodzajPola(RodzajPola.boss);
		fields[5][21].setRodzajPola(RodzajPola.boss);
		fields[5][22].setRodzajPola(RodzajPola.boss);
		fields[6][18].setRodzajPola(RodzajPola.boss);
		fields[6][20].setRodzajPola(RodzajPola.boss);
		fields[6][22].setRodzajPola(RodzajPola.boss);
		fields[7][17].setRodzajPola(RodzajPola.boss);
		fields[7][18].setRodzajPola(RodzajPola.boss);
		fields[7][19].setRodzajPola(RodzajPola.boss);
		fields[7][20].setRodzajPola(RodzajPola.boss);
		fields[7][21].setRodzajPola(RodzajPola.boss);
		fields[7][22].setRodzajPola(RodzajPola.boss);
		fields[7][23].setRodzajPola(RodzajPola.boss);
		fields[8][18].setRodzajPola(RodzajPola.boss);
		fields[8][22].setRodzajPola(RodzajPola.boss);
		fields[9][20].setRodzajPola(RodzajPola.boss);
		fields[9][18].setRodzajPola(RodzajPola.turret_d);
		fields[9][22].setRodzajPola(RodzajPola.turret_d);
		fields[10][20].setRodzajPola(RodzajPola.turret_d);
	}
	
	public void Boss2(){
		fields[5][58].setRodzajPola(RodzajPola.boss2);
		fields[5][59].setRodzajPola(RodzajPola.boss2);
		fields[5][60].setRodzajPola(RodzajPola.boss2);
		fields[5][61].setRodzajPola(RodzajPola.boss2);
		fields[5][62].setRodzajPola(RodzajPola.boss2);
		fields[6][58].setRodzajPola(RodzajPola.boss2);
		fields[6][60].setRodzajPola(RodzajPola.boss2);
		fields[6][62].setRodzajPola(RodzajPola.boss2);
		fields[7][57].setRodzajPola(RodzajPola.boss2);
		fields[7][58].setRodzajPola(RodzajPola.boss2);
		fields[7][59].setRodzajPola(RodzajPola.boss2);
		fields[7][60].setRodzajPola(RodzajPola.boss2);
		fields[7][61].setRodzajPola(RodzajPola.boss2);
		fields[7][62].setRodzajPola(RodzajPola.boss2);
		fields[7][63].setRodzajPola(RodzajPola.boss2);
		fields[8][58].setRodzajPola(RodzajPola.boss2);
		fields[8][62].setRodzajPola(RodzajPola.boss2);
		fields[9][60].setRodzajPola(RodzajPola.boss2);
		fields[9][58].setRodzajPola(RodzajPola.turret_d);
		fields[9][62].setRodzajPola(RodzajPola.turret_d);
		fields[10][60].setRodzajPola(RodzajPola.turret_d);
	}
	
	public void Helicopter(){
		fields[80][95].setRodzajPola(RodzajPola.heli);
	}
	
	public void BaseHealth(){
		fields[90][20].setRodzajPola(RodzajPola.base_health);
		fields[89][19].setRodzajPola(RodzajPola.base_health);
		fields[89][21].setRodzajPola(RodzajPola.base_health);
		fields[91][19].setRodzajPola(RodzajPola.base_health);
		fields[91][21].setRodzajPola(RodzajPola.base_health);
		fields[90][19].setRodzajPola(RodzajPola.base_health);
		fields[90][18].setRodzajPola(RodzajPola.base_health);
		fields[90][21].setRodzajPola(RodzajPola.base_health);
		fields[90][22].setRodzajPola(RodzajPola.base_health);
		fields[91][20].setRodzajPola(RodzajPola.base_health);
		fields[92][20].setRodzajPola(RodzajPola.base_health);
	}
	

	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==timer){
			Move();
		}
	
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
	    int key = e.getKeyCode();

	    if (key == KeyEvent.VK_LEFT) {
	    	typPociskuGracza = TypeOfProjectile.nuke;
	    }	
	    if (key == KeyEvent.VK_RIGHT) {
	    	typPociskuGracza = TypeOfProjectile.bullet;
	    }
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {
	}


}
