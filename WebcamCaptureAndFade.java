package webcamCaptureAndFade;

import javax.swing.*;
import java.io.*;

import javax.imageio.ImageIO;
import javax.media.*;
import javax.media.format.*;
import javax.media.util.*;
import javax.media.control.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import com.sun.image.codec.jpeg.*;

/**
 * WEBCAM CAPTURE AND FADE
 * 
 * This program is made for the science center Vitenfabrikken in Sandnes, Norway.
 * It captures images from a normal webcamera in Windows and fades between
 * the captured images.
 * 
 * The program is made February/March for 2010 for the exhibition "Kem e Sandnes?"
 * ("Who is Sandnes?"). The exhibition will be/is opened on 14th of April.
 *
 *
 * Some features:
 * - A picture is only shown once in the fading images
 * - Pictures captures is saved with the current time and date
 * - Last pictures captured as shown more often than older once 
 *   (number of pictures considered last pictures is controlled by lastadded_max)
 * - With no camera connected, the program won't start (gives a error message)
 * - Debug by pressing t, y or u (more can be added in keyPressed())
 * 
 * Program is written by Hallvard Nygård <hn@jaermuseet.no> for Vitenfabrikken (part of Jærmuseet)
 * 
 * @author Jærmuseet / Vitenfabrikken, Hallvard Nygård <hn@jaermuseet.no>
 *
 */

public class WebcamCaptureAndFade {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static void main(String s[]) {
		JFrame frame = new JFrame();
		frame.setTitle("Webcam capture and imagefading - " +
				"Vitenfabrikken Jærmuseet - " +
				"made by Hallvard Nygard - " +
				"Vitenfabrikken.no");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		WebcamCaptureAndFadePanel panel = new WebcamCaptureAndFadePanel();
		frame.getContentPane().add(panel);
		frame.addKeyListener(panel);
		frame.pack();
		
		//frame.setUndecorated(true);
		
		// Fullscreen:
		/*GraphicsEnvironment.
			getLocalGraphicsEnvironment().
			getDefaultScreenDevice().
			setFullScreenWindow(frame);*/
		
		
		frame.setVisible(true);
		

	}
}

class WebcamCaptureAndFadePanel extends JPanel implements KeyListener, Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static Player player;
	public CaptureDeviceInfo di;
	public MediaLocator ml;
	public JButton capture;
	public JButton capture2;
	public Buffer buf;
	public Image img;
	public VideoFormat vf;
	public BufferToImage btoi;
	public JPanel buttonpanel;
	
	public FormatControl formatControl;
	
	public String saveDirectory = "l:\\webcamtest";
	
	public Boolean gotImages = false;
	
	public WebcamCaptureAndFadeImagePanel[] imagepanels;
	
	public int size_x, size_y;
	
	public WebcamCaptureAndFadePanel() {
		
		// Resolution of the camera pictures divided by 2
		size_x = 320/2;
		size_y = 240/2;
		
		getImages();
		images_used = new ArrayList<Integer>();
		images_lastadded = new ArrayList<Integer>();
		
		imagepanels = new WebcamCaptureAndFadeImagePanel[4];
		imagepanels[0] = new WebcamCaptureAndFadeImagePanel(3,8, size_x, size_y);
		imagepanels[1] = new WebcamCaptureAndFadeImagePanel(2,3, size_x, size_y);
		imagepanels[2] = new WebcamCaptureAndFadeImagePanel(2,3, size_x, size_y);
		imagepanels[3] = new WebcamCaptureAndFadeImagePanel(3,8, size_x, size_y);

		//String str1 = "vfw:Logitech USB Video Camera:0";
		String str2 = "vfw:Microsoft WDM Image Capture (Win32):0";
		di = CaptureDeviceManager.getDevice(str2);
		ml = di.getLocator();
		
		Component comp;
		
		try {
			player = Manager.createRealizedPlayer(ml);
			formatControl = (FormatControl)player.getControl(
            "javax.media.control.FormatControl");
			
			/*
			Format[] formats = formatControl.getSupportedFormats();
			for (int i=0; i<formats.length; i++)
				System.out.println(formats[i].toString());
			*/
			
			player.start();
		}
		catch(javax.media.NoPlayerException e) 
		{
			 JOptionPane.showMessageDialog(null, "Klarer ikke å starte"+
					 " programmet pga. feil med kamera. Sjekk at det er koblet til.", 
					 "IOException", 
					 JOptionPane.ERROR_MESSAGE); 
			 System.exit(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
		/*
		 * Layout
		 * 
		 * Add
		 * - comp
		 * - imagepanels
		 */
		setLayout(new BorderLayout());
		setSize((4+2+4)*size_x, (6)*size_y);
		
		add(imagepanels[0],	BorderLayout.WEST);
		
		JPanel middle = new JPanel(new BorderLayout());
		middle.add(imagepanels[1], BorderLayout.NORTH);
		if ((comp = player.getVisualComponent()) != null) {
			middle.add(comp, BorderLayout.CENTER);
		}
		middle.add(imagepanels[2], BorderLayout.SOUTH);
		middle.setSize(new Dimension(4*size_x, 2*size_y));
		add(middle, BorderLayout.CENTER);
		
		add(imagepanels[3], BorderLayout.EAST);
		
		
		/*
		 * Thread
		 */
		Thread thread = new Thread(this);
		thread.start();
	}
	
	protected void getImages() {
		File directory = new File(this.saveDirectory);

		//BufferedImage img = null;
		
		images = new ArrayList<String>();
		if( directory.exists() && directory.isDirectory())
		{
			//File[] files = directory.listFiles();
			String[] files = directory.list();
		
			for(int i=0; i < files.length; i++){
				//System.out.println(files[i]);
				//try {
					//if(files[i].getName().startsWith("cam"))
					if(files[i].startsWith("cam"))
					{
						// Windows spesific
						images.add(this.saveDirectory + "\\" + files[i]);
						/*
					    img = ImageIO.read(files[i]);
					    images.add(img);*/
					}
				//} catch (IOException e) {
				//	System.err.println("Failed on reading image. IOException.");
				//}
			}
		}
		
		System.out.println("Total image count = " + images.size());
		gotImages = true;
	}
	
	public Image getImage(int imagenum)
	{
		String path = images.get(imagenum);
		try {
			return ImageIO.read(new File(path));
		} catch (IOException e) {
			System.out.println("Path til ikke funnet: " + path);
			return null;
		}
	}
	
	public static void playerclose() {
		player.close();
		player.deallocate();
	}
	
	public void captureImage()
	{
		String savepath = this.saveDirectory + "\\cam"
		+ this.getDateFormatNow("yyyyMMdd_HHmmss-S") + ".jpg";
		System.out.println("Capturing current image to " +savepath);
		
		// Grab a frame
		FrameGrabbingControl fgc = (FrameGrabbingControl) player
				.getControl("javax.media.control.FrameGrabbingControl");
		buf = fgc.grabFrame();

		// Convert it to an image
		btoi = new BufferToImage((VideoFormat) buf.getFormat());
		img = btoi.createImage(buf);
		
		// save image
		saveJPG(img, savepath);
		
		// show the image
		//imgpanel.setImage(img);
		
		//images.add(img);
		images.add(savepath);
		
		if(images_lastadded.size() >= lastadded_max)
		{
			// Remove last
			images_lastadded.remove(images_lastadded.size()-1);
		}
		
		images_lastadded.add(0, images.size()-1);
	}
	
	public void nextFrame() {
		for (int i = 0; i < imagepanels.length; i++) {
			imagepanels[i].nextFrame();
		}
	}

	public static void saveJPG(Image img, String s) {
		BufferedImage bi = new BufferedImage(
				img.getWidth(null), 
				img.getHeight(null), 
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		g2.drawImage(img, null, null);

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(s);
		} catch (java.io.FileNotFoundException io) {
			System.out.println("File Not Found");
		}

		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bi);
		param.setQuality(0.5f, false);
		encoder.setJPEGEncodeParam(param);

		try {
			encoder.encode(bi);
			out.close();
		} catch (java.io.IOException io) {
			System.out.println("IOException");
		}
	}

	public ArrayList<String> images;
	public ArrayList<Integer> images_used;
	
	public ArrayList<Integer> images_lastadded; // Last added, intergers refering to images
	public int lastadded_max = 20; // How many images is considered "lastadded"
	public int randomImageNum_maxTries = 100;
	
	public int lastImg = 0;
	
	public int getRandomImageNum () {
		if(images.size() == images_used.size())
			return -1;
		else
		{
			int i;
			int j = 0;
			int tries = 0; 
			while(true && tries < randomImageNum_maxTries)
			{
				// Always try the last added pictures
				if (j < images_lastadded.size())
				{
					i = images_lastadded.get(j++);
				}
				else
				{
					// Random from the rest of the pictures
					i = (int)(Math.random() * images.size());
					tries++;
				}
				if(!images_used.contains((Integer)i))
					return i;
			}
			
			System.out.println("Max tries in randomImageNum");
			return -1;
		}
		
		// Original:
		//return (int)(Math.random() * images.size());
	}

	class WebcamCaptureAndFadeImagePanel extends JPanel
	{
	
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		// Grid of images, x and y
		public int grid_x;
		public int grid_y;
		
		// Size for each image, x and y
		public int size_x;
		public int size_y;

		public float[][] fade; // How much fading
		public int[][] wait; // Counter for 

		public int[][]imagenum_now; // Images for the panel, showing now
		public int[][]imagenum_next; // Images for the panel, fading in
		//int refers to images in parent object
		
		public Image[][]imagenum_now2;
		public Image[][]imagenum_next2;
		
		public WebcamCaptureAndFadeImagePanel (int grid_x, int grid_y, int size_x, int size_y) {
			
			// Set to object
			this.grid_x = grid_x;
			this.grid_y = grid_y;
			
			this.size_x = size_x;
			this.size_y = size_y;
			
			// init tables
			imagenum_now    = new int[this.grid_x][this.grid_y];
			imagenum_next   = new int[this.grid_x][this.grid_y];
			imagenum_now2   = new Image[this.grid_x][this.grid_y];
			imagenum_next2  = new Image[this.grid_x][this.grid_y];
			fade = new float[this.grid_x][this.grid_y];
			wait = new int[this.grid_x][this.grid_y];
			
			
			for (int i = 0; i < imagenum_now.length; i++) {
				for (int j = 0; j < imagenum_now[i].length; j++) {
					imagenum_now[i][j]    = getRandomImageNum();
					images_used.add((Integer)imagenum_now[i][j]);
					imagenum_next[i][j]   = getRandomImageNum();
					images_used.add((Integer)imagenum_next[i][j]);
					
					fade[i][j]            = 0.1f*i*j;

					imagenum_now2[i][j]   = getImage(imagenum_now[i][j]);
					imagenum_next2[i][j]  = getImage(imagenum_next[i][j]);
				}
			}
			
			setPreferredSize(
					new Dimension(
						this.grid_x*size_x, 
						this.grid_y*size_y
					)
				);
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			Graphics2D g2 = (Graphics2D) g;

			AlphaComposite ac, ac2 = null;
			ac2 = AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER,
					1.0f
				);
			for (int i = 0; i < imagenum_now.length; i++) {
				for (int j = 0; j < imagenum_now[i].length; j++) {
					//System.out.println("x: " + i + ", y: " + j);
					
					g2.setComposite(ac2);
					if(imagenum_now[i][j] != -1)
					{
						g2.drawImage(
								//images.get(imagenum_now[i][j])
								//getImage(images.get(imagenum_now[i][j])),
								imagenum_now2[i][j],
								i*size_x,
								j*size_y,
								size_x,
								size_y,
								this);
					}
					else
					{
						// No picture found, ignoring
						//System.out.println("imagenum_now["+i+"]["+j+"] = -1");
					}
					
					try {
						ac = AlphaComposite.getInstance(
							AlphaComposite.SRC_OVER,
							fade[i][j]
							);
					}
					catch (IllegalArgumentException e) {
						ac = AlphaComposite.getInstance(
								AlphaComposite.SRC_OVER,
								1.0f
								);
					}
					g2.setComposite(ac);
					if(imagenum_next[i][j] != -1)
					{
						g2.drawImage(
								//images.get(imagenum_next[i][j]),
								//getImage(images.get(imagenum_next[i][j])), 
								imagenum_next2[i][j],
								i*size_x,
								j*size_y,
								size_x,
								size_y,
								this);
					}
					else
					{
						// No picture found, ignoring
						//System.out.println("imagenum_now["+i+"]["+j+"] = -1");
					}
					
					/*
					if(i == 0 && j == 0)
						System.out.println("" + imagenum_now[i][j] + 
								" => " + 
								imagenum_next[i][j] + ", fade: "+fade[i][j]);;
					*/
				}
			}
		}
		
		public void nextFrame() {
			int tmp;
			for (int i = 0; i < imagenum_next.length; i++) {
				
				for (int j = 0; j < imagenum_next[i].length; j++) {
					
					if(fade[i][j] >= 1.0f)
					{
						// Finished fading => Start on the next image
						tmp = getRandomImageNum();
						images_used.remove((Integer)imagenum_now[i][j]);
						
						imagenum_now[i][j]  = imagenum_next[i][j];
						imagenum_now2[i][j] = imagenum_next2[i][j];
						
						imagenum_next[i][j] = tmp;
						imagenum_next2[i][j] = getImage(tmp);
						
						images_used.add((Integer)imagenum_next[i][j]);
						fade[i][j] = 0.0f;
						wait[i][j] = 10; // Number of frames to wait
					}
					else if (wait[i][j] <= 0)
					{
						fade[i][j] += 0.05f;
					}
					else
					{
						wait[i][j]--; // Wait a little longer
					}
					
				}
			}
			repaint();
		}
		
	}

	@Override
	public void run() {
		while (true) {
			this.nextFrame();
			
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == 67) // C 
		{
			this.captureImage();
		}
		
		else if(e.getKeyCode() == 27) // Escape
		{
			System.out.println("Escape pressed, exiting");
			System.exit(0);
		}
		
		else if(e.getKeyCode() == 84) // t
		{
			// Testing purpose
			for (int i = 0; i < images.size(); i++) {
				System.out.println("image " + i + ", used? " + 
						images_used.contains((Integer)i));
			}
			
		}
		
		else if(e.getKeyCode() == 89) // y
		{
			// Testing purpose
			System.out.println("getRandomImageNum() = " + getRandomImageNum());
		}
		
		else if(e.getKeyCode() == 73) // i
		{
			// Testing purpose
			System.out.println("LAST ADDED");
			for (int i = 0; i < images_lastadded.size(); i++) {
				System.out.println(i + " - image " + images_lastadded.get(i) + ", used? " + 
						images_used.contains(
								(Integer)images_lastadded.get(i)
							));
			}
		}
		
		else if(e.getKeyCode() == 85) // u
		{
			// Testing purpose
			for (int i = 0; i < imagepanels.length; i++) {
				for (int j = 0; j < imagepanels[i].imagenum_now.length; j++) {
					for (int j2 = 0; j2 < imagepanels[i].imagenum_now[j].length; j2++) {
						String print1;
						if(imagepanels[i].imagenum_now[j][j2] < 10)
							print1 = "  " + imagepanels[i].imagenum_now[j][j2];
						else if(imagepanels[i].imagenum_now[j][j2] < 100)
							print1 = " " + imagepanels[i].imagenum_now[j][j2];
						else
							print1 = "" + imagepanels[i].imagenum_now[j][j2];
						String print2;
						if(imagepanels[i].imagenum_next[j][j2] < 10)
							print2 = "  " + imagepanels[i].imagenum_next[j][j2];
						else if(imagepanels[i].imagenum_next[j][j2] < 100)
							print2 = " " + imagepanels[i].imagenum_next[j][j2];
						else
							print2 = "" + imagepanels[i].imagenum_next[j][j2];
						
						System.out.println("imagepanels["+i+"]." +
								"imagenum_now["+j+"]["+j2+"] = " +
								print1 + 
								", next = " + print2);
					}
				}
			}
		}
		
		else {
			displayInfo(e, "KEY TYPED: ");
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		
	}
	
	private void displayInfo(KeyEvent e, String keyStatus) {
		// Method copied from http://java.sun.com/docs/books/tutorial/uiswing/events/keylistener.html

		//You should only rely on the key char if the event
		//is a key typed event.
		int id = e.getID();
		String keyString;
		if (id == KeyEvent.KEY_TYPED) {
			char c = e.getKeyChar();
			keyString = "key character = '" + c + "'";
		} else {
			int keyCode = e.getKeyCode();
			keyString = "key code = " + keyCode + " ("
					+ KeyEvent.getKeyText(keyCode) + ")";
		}

		int modifiersEx = e.getModifiersEx();
		String modString = "extended modifiers = " + modifiersEx;
		String tmpString = KeyEvent.getModifiersExText(modifiersEx);
		if (tmpString.length() > 0) {
			modString += " (" + tmpString + ")";
		} else {
			modString += " (no extended modifiers)";
		}

		String actionString = "action key? ";
		if (e.isActionKey()) {
			actionString += "YES";
		} else {
			actionString += "NO";
		}

		String locationString = "key location: ";
		int location = e.getKeyLocation();
		if (location == KeyEvent.KEY_LOCATION_STANDARD) {
			locationString += "standard";
		} else if (location == KeyEvent.KEY_LOCATION_LEFT) {
			locationString += "left";
		} else if (location == KeyEvent.KEY_LOCATION_RIGHT) {
			locationString += "right";
		} else if (location == KeyEvent.KEY_LOCATION_NUMPAD) {
			locationString += "numpad";
		} else { // (location == KeyEvent.KEY_LOCATION_UNKNOWN)
			locationString += "unknown";
		}

		// Added:
		System.out.println("Keypress: " + keyString);
	}
	
	public String getDateFormatNow(String dateFormat)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		return sdf.format(Calendar.getInstance().getTime());
	}
}