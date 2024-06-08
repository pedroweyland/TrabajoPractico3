package ar.edu.utn.frbb.tup.service;

import ar.edu.utn.frbb.tup.model.*;
import ar.edu.utn.frbb.tup.model.exception.CuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaNoSoportadaException;
import ar.edu.utn.frbb.tup.persistence.ClienteDao;
import ar.edu.utn.frbb.tup.persistence.CuentaDao;
import net.bytebuddy.build.ToStringPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CuentaServiceTest {
    @Mock
    private CuentaDao cuentaDao;

    @Mock
    private ClienteDao clienteDao;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private CuentaService cuentaService;

    @BeforeAll
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCuentaCreadaSucces() throws TipoCuentaAlreadyExistsException, CuentaAlreadyExistsException, TipoCuentaNoSoportadaException {
        Cliente cliente = getCliente(123456789L, "Pepo");
        Cuenta cuenta = getCuenta(cliente, TipoMoneda.PESOS, TipoCuenta.CUENTA_CORRIENTE);

        cuentaService.darDeAltaCuenta(cuenta, cliente.getDni());

        verify(clienteService, times(1)).agregarCuenta(cuenta, cliente.getDni());
        verify(cuentaDao, times(1)).save(cuenta);
    }

    @Test
    public void testCuentaExistente(){
        Cliente cliente = getCliente(123456789L, "Pepo");
        Cuenta cuenta = getCuenta(cliente, TipoMoneda.PESOS, TipoCuenta.CUENTA_CORRIENTE);

        when(cuentaDao.find(cuenta.getNumeroCuenta())).thenReturn(cuenta);

        assertThrows(CuentaAlreadyExistsException.class, () -> cuentaService.darDeAltaCuenta(cuenta, cliente.getDni()));

        verify(cuentaDao, times(1)).find(cuenta.getNumeroCuenta());
    }

    @Test
    public void testCuentaTipoCuentaNoSoportada(){
        Cliente cliente = getCliente(123456789L, "Pepo");
        Cuenta cuenta = getCuenta(cliente, TipoMoneda.DOLARES, TipoCuenta.CUENTA_CORRIENTE); //Creo una cuenta que no va ser soportada

        //Queremos que retorne null ya que no queremos que nos tire la excepcion CuentaAlreadyExistesExcepcuion
        when(cuentaDao.find(cuenta.getNumeroCuenta())).thenReturn(null);

        assertThrows(TipoCuentaNoSoportadaException.class, () -> cuentaService.darDeAltaCuenta(cuenta, cliente.getDni()));
    }

    @Test
    public void testCuentaMismoTipo() throws TipoCuentaNoSoportadaException, TipoCuentaAlreadyExistsException, CuentaAlreadyExistsException {
        Cliente cliente = getCliente(123456789L, "Pepo");
        Cuenta cuenta = getCuenta(cliente, TipoMoneda.PESOS, TipoCuenta.CUENTA_CORRIENTE);

        when(cuentaDao.find(cuenta.getNumeroCuenta())).thenReturn(null);

        Cuenta resultado = cuentaDao.find(cuenta.getNumeroCuenta());

        verify(cuentaDao, times(1)).find(cuenta.getNumeroCuenta());
        assertNull(resultado);

        cuentaService.darDeAltaCuenta(cuenta, cliente.getDni());

        Cuenta cuentaRepetida = getCuenta(cliente, TipoMoneda.PESOS, TipoCuenta.CUENTA_CORRIENTE);

        assertThrows(TipoCuentaNoSoportadaException.class, () -> cuentaService.darDeAltaCuenta(cuentaRepetida, cliente.getDni()));

        verify(cuentaDao, times(1)).find(cuenta.getNumeroCuenta());
        assertEquals(1, cliente.getCuentas().size());
    }

    public Cliente getCliente(long dni, String nombre){
        Cliente cliente = new Cliente();
        cliente.setDni(dni);
        cliente.setNombre(nombre);
        cliente.setApellido("Rino");
        cliente.setFechaNacimiento(LocalDate.of(1978, 3,25));
        cliente.setTipoPersona(TipoPersona.PERSONA_FISICA);

        return cliente;
    }

    public Cuenta getCuenta(Cliente titular, TipoMoneda tipoMoneda, TipoCuenta tipoCuenta){
        Cuenta cuenta = new Cuenta();

        cuenta.setTitular(titular);
        cuenta.setMoneda(tipoMoneda);
        cuenta.setTipoCuenta(tipoCuenta);
        cuenta.setBalance(5000);

        return cuenta;
    }
}
