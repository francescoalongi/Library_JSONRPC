public class ClassWithMethods {

    public int sum (String num1, String num2) {
        System.out.println("Method sum has been called.");
        return Integer.parseInt(num1)+Integer.parseInt(num2);
    }
    public void notify_im_up() {
        System.out.println("Ok, now I know you are up.");
    }
    public void foo () {
        System.out.println("Method foo has been called.");
    }

    public void methodWithException () {
        System.out.println("Ok, I catched an exception here.");
        throw new IllegalArgumentException("the method you called is made just for throwing exception!");
    }
}
