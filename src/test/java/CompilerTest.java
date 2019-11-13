import org.junit.jupiter.api.Test;

class CompilerTest {

    @Test
    void compile() throws Exception {
        new Compiler("com.xxx.abc.api", "com.xxx.framework.rpc")
                .writeToFile(
                        XXX1Service.class,
                        XXX2Service.class,
                        XXX3Service.class
                );
    }
}
