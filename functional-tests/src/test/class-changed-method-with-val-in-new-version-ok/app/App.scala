object App {
  def main(args: Array[String]): Unit = {
    println(new A().foo)
    println(new A { override def foo = 3 }.foo)
    println(new A { override val foo = 4 }.foo)
  }
}
