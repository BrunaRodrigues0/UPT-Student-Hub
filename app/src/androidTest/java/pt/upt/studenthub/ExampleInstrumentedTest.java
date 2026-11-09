package pt.upt.studenthub;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Teste instrumentado, executado num dispositivo Android.
 *
 * @see <a href="http://d.android.com/tools/testing">Documentação de testes</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Contexto da aplicação em teste.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("pt.upt.studenthub", appContext.getPackageName());
    }
}