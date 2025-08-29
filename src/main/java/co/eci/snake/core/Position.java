package co.eci.snake.core;

public record Position(int x, int y) {
  public Position wrap(int width, int height) {
    int nx = ((x % width) + width) % width;
    int ny = ((y % height) + height) % height;
    return new Position(nx, ny);
  }
}