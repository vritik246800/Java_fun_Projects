class App 
{
    int x=9,y=8;
    void main() {
        int c=0;
        Object[] arr = new Object[somar()];
        for (Object elem : arr) 
            IO.println(c+++" Hello World!");
               
    }
    int somar(){
        return x+y;
    }
}