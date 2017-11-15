import java.awt.EventQueue;

import java.awt.EventQueue;
import javax.swing.JFrame;



public class Main extends JFrame{
	private static final long serialVersionUID = 1L;
	
	private JFrame frame;
	private Board board = new Board();

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public Main(){
		initialize();
	}
	
	
	private void initialize() {		
		frame = new JFrame("Okno");
		frame.setBounds(200, 0, 1000, 730);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.getContentPane().add(board);
	}
	
	

}
