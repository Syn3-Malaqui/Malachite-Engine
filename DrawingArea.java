public class DrawingArea {
    public final int x;
    
    public final int y;
    
    public final int width;
    
    public final int height;
    
    /**
     * Constructs a new DrawingArea with the specified position and dimensions.
     * 
     * @param x The x-coordinate of the top-left corner
     * @param y The y-coordinate of the top-left corner
     * @param width The width of the drawing area
     * @param height The height of the drawing area
     */
    public DrawingArea(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
} 