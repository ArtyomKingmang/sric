//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author yangjiandong
 */
public class GenericTest {
    @Test
    public void test() throws IOException {
        String file = "res/code/testGeneric.sric";
        String libPath = "../lib";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
}
