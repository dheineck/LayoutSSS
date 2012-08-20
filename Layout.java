import java.util.Arrays;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Color;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class Layout {

	// list of nets connected each net
	public static int[][] netlist; 
	// order to place a net, according to heuristic	
	public static int[] order;	
	// a list of all components
	public static Component[] compList;	
	// edges available for placement
	public static ArrayList<Edge> availEdges = new ArrayList<Edge>();
	// components that have been already placed
	public static ArrayList<Component> placed = new ArrayList<Component>();
	// all connections between placed components
	public static ArrayList<Connection> connections = new ArrayList<Connection>();
	// parameter for how far away to place a component from an existing component
	// if it does not connect to any currently placed components 
	public static int NON_CONN_DIST = 200;


	public static void main(String[] args) {

		// load the netlist from file
		if (args.length > 0) load_netlist(args[0]);
		else load_netlist("netlist.txt");

		for (int n = 0; n < order.length; n++) {		
			Component c = new Component(order[n]);
			compList[order[n]] = c;
			place(c);
		}

		display();
		if (args.length > 1) write_results(args[1]);
		else write_results("placement_results.txt");
	}

	public static void display() {
		int minx = 0;		
		int miny = 0;
		for (Component c : placed) {
			System.out.println(c);
			if (c.cent[0]-c.w/2 < minx) 
				minx = c.cent[0]-c.w/2;
			if (c.cent[1]-c.h/2 < miny)
				miny = c.cent[1]-c.h/2;
		}
		for (Component c : placed) {
			c.cent[0] += (-1*minx);
			c.cent[1] += (-1*miny);
		}

		JFrame frame = new JFrame();
		frame.add(new Plot(placed));
		frame.setSize(400,400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.show(true);
	}

	public static void place(Component c) {
		//check available edges -- distance to components (immediate only for now?)
		// Find a candidate?  Check for collisions
		// Add free edges to available edges list
		
		/*
		* General Algorithm:		
		* AvailEdges are all open edges for placed components
		* c is the component being placed
		* for all edges e in AvailEdges, select the edge of c that faces e
		* place c orthagonal to e, centered at the average of the orthogonal coordinates 
		* let m be the best wire distance from c to all connected components
		* let d be the current wire distance from c at the current edge e
		* if d < m, then d <- m
		*/

		// if there are no free edges, place component at 0,0
		if (availEdges.size() == 0) {
			c.cent[0] = 0;
			c.cent[1] = 0;
			placed.add(c);
			for (int d = 0; d < 360; d+=90)			
				availEdges.add(new Edge(c, d));
			System.out.println("No avail, Successfully placed "+c.num+": "+0+","+0+" "+c.w+"x"+c.h);
		}
		else {
			ArrayList<Integer> connectList = new ArrayList<Integer>();
			// Look at this component and what it needs to connect with
			// Look at the list of what's already been placed
			// Build a list of what it can currently connect with
			for (int i = 0; i < netlist[c.num].length; i++) {
				for (int j = 0; j < placed.size(); j++) {
					if (netlist[c.num][i] == placed.get(j).num)
						connectList.add(placed.get(j).num);
				}
			}

			// if there are placed components that need connections
			System.out.print(connectList.size() + " connections: ");
			if (connectList.size() > 0) {
				int x = 0;
				int y = 0;
				double bestDist = Double.MAX_VALUE;
				int bestEdgeIndex = 0;

				// look through the available edges to see what has best distance
				for (int i = 0; i < availEdges.size(); i++) {

					// check for connection candidates among the available edges
					if (connectList.contains(availEdges.get(i).comp.num)) {
						Edge edge = availEdges.get(i);					
						int tempx = 0;
						int tempy = 0;
						double tempDist = 0;
						int dir = edge.freeDir;
						
						// calc dist to each connected placed comp from this point	
						for (int j = 0; j < connectList.size(); j++) {
							Component connect_comp = compList[connectList.get(j)];

							if (dir == 0 || dir == 90 || dir == 180)
								tempx = edge.points[0][0] + c.w / 2;
							if (dir == 270)
								tempx = edge.points[0][0] - c.w / 2;
							if (dir == 0)
								tempy = edge.points[0][1] - c.h / 2;
							if (dir == 90 || dir == 180 || dir == 270)
								tempy = edge.points[0][1] + c.h / 2;

							tempDist += Math.sqrt( Math.pow(tempx-connect_comp.cent[0],2) 
											 + Math.pow(tempy-connect_comp.cent[1],2) );
						}

						// check for overlap& slide as needed
						// for r` in R
						// check if x dist < x dist and y dist < y dist
						// if yes, slide in perp direction from edge's free direction
							// determine which way to move it, and by how much

						for (Component p : placed) {
							double xDis = tempx-p.cent[0];
							int widths = c.w/2 + p.w/2;
							double yDis = tempy-p.cent[1];
							int heights = c.h/2 + p.h/2;
							if (Math.abs(xDis) < widths && Math.abs(yDis) < heights) {
								System.out.print("attemp placing "+c.num+": "+tempx+","+tempy); 
								System.out.println(" on "+p.cent[0]+","+p.cent[1]);

								if (dir == 0 || dir == 180) {
									tempx += xDis;
								}
								if (dir == 90 || dir == 270) {
									tempy += yDis;
								}
								for (int j = 0; j < connectList.size(); j++) {
									Component connect_comp = compList[connectList.get(j)];
									tempDist += Math.sqrt( Math.pow(tempx-connect_comp.cent[0],2) 
										    		 + Math.pow(tempy-connect_comp.cent[1],2) );
								}
								xDis = tempx-p.cent[0];
								widths = c.w/2 + p.w/2;
								yDis = tempy-p.cent[1];
								heights = c.h/2 + p.h/2;
								if (Math.abs(xDis) < widths && Math.abs(yDis) < heights) {
									tempDist = Double.MAX_VALUE;
									// should slide and recalc, but this'll work for now
								}
							}
						}

						if (tempDist < bestDist) {
							bestDist = tempDist;
							bestEdgeIndex = i;
							x = tempx;
							y = tempy;
						}
					}
				} 
				Edge filled = availEdges.remove(bestEdgeIndex);
				c.cent[0] = x;
				c.cent[1] = y;
				placed.add(c);
				for (int d = 0; d < 360; d+=90)			
					if (d != (filled.freeDir+180)%360) 
						availEdges.add(new Edge(c, d));
				System.out.println("Successfully placed "+c.num+": "+x+","+y+" "+c.w+"x"+c.h);
				// still haven't checked if other directions are occupied
			}

			// placing a new component and there's nothing to connect with
			if (connectList.size() == 0) {
				int maxy = 0;			
				int miny = 0;
				int maxx = 0;
				int minx = 0;
	
				// find the "bottom-most" component 
				for (int i = 0; i < placed.size(); i++) {
					if (placed.get(i).cent[1]+placed.get(i).h/2 > maxy)
						maxy = placed.get(i).cent[1]+placed.get(i).h/2;
					if (placed.get(i).cent[1]-placed.get(i).h/2 < miny)
						miny = placed.get(i).cent[1]-placed.get(i).h/2;
					if (placed.get(i).cent[0]+placed.get(i).w/2 > maxx)
						maxx = placed.get(i).cent[0]+placed.get(i).w/2;
					if (placed.get(i).cent[0]-placed.get(i).w/2 < minx)
						minx = placed.get(i).cent[0]-placed.get(i).w/2;
				}
				int rand = (int)(Math.random()*4);
				if (rand == 0){
					c.cent[0] = 0;
					c.cent[1] = maxy + c.h/2;//NON_CONN_DIST;
				}
				if (rand == 1){
					c.cent[0] = 0;
					c.cent[1] = miny - c.h/2;//NON_CONN_DIST;
				}
				if (rand == 2){
					c.cent[0] = maxx + c.w/2;//NON_CONN_DIST;
					c.cent[1] = 0;
				}
				if (rand == 3){
					c.cent[0] = minx - c.w/2;//NON_CONN_DIST;
					c.cent[1] = 0;
				}				
				System.out.print("max "+maxx+","+maxy+" "+rand);
				System.out.print(" min "+minx+","+miny+" ");
				placed.add(c);
				for (int d = 0; d < 360; d+=90)			
					availEdges.add(new Edge(c, d));
				System.out.println(" Successfully placed "+c.num+": "
							+c.cent[0]+","+c.cent[1]+" "+c.w+"x"+c.h);
			}
		}
	}


	public static double[] getNewNodeDist(ArrayList<Integer> connectList, 
								   Edge edge, Component c) {
		double dist = 0;	
		int tempx = 0;
		int tempy = 0;	
		int dir = edge.freeDir;
		for (int j = 0; j < connectList.size(); j++) {
			Component connect_comp = compList[connectList.get(j)];

			if (dir == 0 || dir == 90 || dir == 180)
				tempx = edge.points[0][0] + c.w / 2;
			if (dir == 270)
				tempx = edge.points[0][0] - c.w / 2;
			if (dir == 0)
				tempy = edge.points[0][1] - c.h / 2;
			if (dir == 90 || dir == 180 || dir == 270)
				tempy = edge.points[0][1] + c.h / 2;

			dist += Math.sqrt( Math.pow(tempx-connect_comp.cent[0],2) 
				    		 + Math.pow(tempy-connect_comp.cent[1],2) );
		}
		return new double[] {tempx, tempy, dist};
	}

	public static void write_results(String fileName) {

		try {
			File dataFile = new File(fileName);
			FileWriter fileWriter = new FileWriter(dataFile);
			BufferedWriter writer = new BufferedWriter(fileWriter);

			System.out.println("Writing " + compList.length + " results...");

			for (Component c : compList) {
				writer.write(c.toStringAll() + "\n");
			}

			writer.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}


	}

	public static void load_netlist(String fileName) {

		ArrayList<String[]> nets = new ArrayList<String[]>();
		int count = 0;			// number of nets
		int max_list_size = 0;	// greatest number of connections for a net

		// read the file, put contents into nets ArrayList
		try {
			File dataFile = new File(fileName);
			FileReader fileReader = new FileReader(dataFile);
			BufferedReader reader = new BufferedReader(fileReader);

			String line = "";
			while ((line = reader.readLine()) != null) {				
				nets.add(line.split(" "));
				if (nets.get(count).length > max_list_size)
					max_list_size = nets.get(count).length;
				count++;
			}
			reader.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		// populate netlist array
		netlist = new int[count][max_list_size];
		order = new int[count];
		for (int i = 0; i < nets.size(); i++) {
			for (int j = 0; j < nets.get(i).length; j++)
				netlist[i][j] = Integer.parseInt(nets.get(i)[j]);
			order[i] = i;	// initialize the order array unsorted
		}

		// generate the order using the most connected first heuristic
		int most_connections = 0;
		int i_of_most = 0;		
		for (int i = 0; i < netlist.length; i++) {
			most_connections = 0;
			i_of_most = i;
			for (int j = i; j < netlist.length; j++) {
				if (nets.get(order[j]).length > most_connections) {
					most_connections = nets.get(order[j]).length;
					i_of_most = j;
				}	
			}
			int swap = order[i];
			order[i] = order[i_of_most];
			order[i_of_most] = swap;
		}

		// initialize the array that will contain all the components
		compList = new Component[count];
	}
	
}

class Component {
	public int num;
	public int[] cent = new int[2];
	public int w;
	public int h;	
	public Component(int num) {
		this.num = num;
		w = (int)(Math.random()*9)*100+100;
		h = (int)(Math.random()*9)*100+100;
	}
	public String toString() {
		return num+"  "+cent[0]+","+cent[1];
	}
	public String toStringAll() {
		return num+" "+cent[0]+" "+cent[1]+" "+w+" "+h;
	}
}

class Connection {
	public Component[] c;
	public double len;
}

class Edge {
	public Component comp;
	public int[][] points = new int[2][2];
	public int freeDir;	
	public Edge(Component c, int direction) {
		comp = c;
		freeDir = direction;
		if (direction == 270 || direction == 90) {
			// y values w/ horizontal edge
			points[0][1] = c.cent[1] - c.h/2;
			points[1][1] = c.cent[1] + c.h/2;
			
			if (direction == 270) {
				// x values to the left w/ left edge				
				points[0][0] = c.cent[0] - c.w/2;
				points[1][0] = c.cent[0] - c.w/2;
			}

			if (direction == 90) {
				// x values to the right w/ right edge
				points[0][0] = c.cent[0] + c.w/2;
				points[1][0] = c.cent[0] + c.w/2;		
			}
		}
		if (direction == 0 || direction == 180) {
			// x values w/ vertical edge
			points[0][0] = c.cent[0] - c.w/2;
			points[1][0] = c.cent[0] + c.w/2;			

			if (direction == 180) {
				// y values down w/ bottom edge 
				points[0][1] = c.cent[1] + c.h/2;
				points[1][1] = c.cent[1] + c.h/2;
			}

			if (direction == 0) {
				// y values up / top edge
				points[0][1] = c.cent[1] - c.h/2;
				points[1][1] = c.cent[1] - c.h/2;				
			}

		}
	}
	public String toString() {
		return "("+points[0][0]+","+points[0][1]+")-("
			  +points[1][0]+","+points[1][1]+") "+comp.num;
	}
}

class Plot extends JComponent {
	
	public ArrayList<Component> components;
	public Plot(ArrayList<Component> list) {
		components = list;
	}
	public void paint(Graphics g) {
		int s = 6;
		int rd = 0; int gn = 0; int bu = 0;		

		for (Component c : components) {
			g.setColor(new Color(rd, gn, bu));
			g.drawRect((c.cent[0]-c.w/2)/s, (c.cent[1]-c.h/2)/s, c.w/s, c.h/s);
			g.drawString(""+c.num, (c.cent[0]-c.w/2+5)/s, (c.cent[1]+10)/s);
			rd += (int)(Math.random()*25); 
			gn += (int)(Math.random()*25); 
			bu += (int)(Math.random()*25);
		}
		if (false && components.size() == Layout.netlist.length) {
			g.setColor(Color.GRAY);				
			for (int i = 0; i < Layout.netlist.length; i++) {
				for (int j = 0; j < Layout.netlist[i].length; j++) {
					Component a = Layout.compList[i];
					Component b = Layout.compList[j];								
					g.drawLine((a.cent[0]+a.w/2)/s, (a.cent[1]+a.h/2)/s, 
							 (b.cent[0]+b.w/2)/s, (b.cent[1]+b.h/2)/s);
				}
			}
		}
	}
}
