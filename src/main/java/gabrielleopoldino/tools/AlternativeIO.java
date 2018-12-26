package gabrielleopoldino.tools;

public interface AlternativeIO {

    byte[] receive();

    void send(byte[] array, int offset, int len);

}
