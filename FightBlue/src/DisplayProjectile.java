import java.awt.Color;
import java.awt.Rectangle;
import javax.swing.JPanel;

import Rodzaje.RodzajPola;
import Rodzaje.RodzajPola;

public class DisplayProjectile {
	private Color color;
	private Rectangle rectangle;
	private RodzajPola rodzajPola;
	
	public DisplayProjectile(int x, int y, int width, int height, Color color){
		rectangle = new Rectangle(x, y, width, height);
	    setColor(color);
	}
	
	
	public void setColor(Color color){
		this.color = color;
	}
	
	public Color getColor(){
		return color;
	}
	
	public void setRodzajPola(RodzajPola rodzajPola){
		this.rodzajPola = rodzajPola;
	}
	
	public RodzajPola getRodzajPola() {
		return rodzajPola;
	}
	
    public Rectangle getBounds() {
        return rectangle;
    }

}
