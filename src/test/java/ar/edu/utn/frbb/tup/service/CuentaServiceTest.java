package ar.edu.utn.frbb.tup.service;

import ar.edu.utn.frbb.tup.model.*;
import ar.edu.utn.frbb.tup.model.exception.CuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaNoSoportadaException;
import ar.edu.utn.frbb.tup.persistence.CuentaDao;
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
    private ClienteService clienteService;

    @InjectMocks
    private CuentaService cuentaService;

    @BeforeAll
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    //1. Cuenta existente → debería fallar con la exception indicada
    @Test
    public void testCuentaExistente(){
        Cliente cliente = getCliente(123456789L, "Pepo");
        Cuenta cuenta = getCuenta(cliente, TipoMoneda.PESOS, TipoCuenta.CUENTA_CORRIENTE);

        when(cuentaDao.find(cuenta.getNumeroCuenta())).thenReturn(cuenta);

        assertThrows(CuentaAlreadyExistsException.class, () -> cuentaService.darDeAltaCuenta(cuenta, cliente.getDni()));

        verify(cuentaDao, times(1)).find(cuenta.getNumeroCuenta());
    }

    //2. Cuenta no soportada → debería fallar con una exception que deben generar
    @Test
    public void testCuentaTipoCuentaNoSoportada(){
        Cliente cliente = getCliente(123456789L, "Pepo");
        Cuenta cuenta = getCuenta(cliente, TipoMoneda.DOLARES, TipoCuenta.CUENTA_CORRIENTE); //Creo una cuenta que no va ser soportada

        //Queremos que retorne null ya que no queremos que nos tire la excepcion CuentaAlreadyExistesExcepcuion
        when(cuentaDao.find(cuenta.getNumeroCuenta())).thenReturn(null);

        assertThrows(TipoCuentaNoSoportadaException.class, () -> cuentaService.darDeAltaCuenta(cuenta, cliente.getDni()));
    }

    //3. Cliente ya tiene cuenta de ese tipo → debería fallar en este caso el cliente service
    //   (qué debe hacerse en para esto?)
    @Test
    public void testCuentaMismoTipo() throws TipoCuentaAlreadyExistsException, CuentaAlreadyExistsException, TipoCuentaNoSoportadaException {
        Cliente cliente = getCliente(123456789L, "Pepo");
        Cuenta cuenta = getCuenta(cliente, TipoMoneda.PESOS, TipoCuenta.CUENTA_CORRIENTE);
        Cuenta cuentaRepetida = getCuenta(cliente, TipoMoneda.PESOS, TipoCuenta.CUENTA_CORRIENTE);

        when(cuentaDao.find(cuenta.getNumeroCuenta())).thenReturn(null);

        Cuenta resultado = cuentaDao.find(cuenta.getNumeroCuenta());
        assertNull(resultado);

        cuentaService.darDeAltaCuenta(cuenta, cliente.getDni());

        doThrow(TipoCuentaAlreadyExistsException.class).when(clienteService).agregarCuenta(cuentaRepetida, cliente.getDni());

        assertThrows(TipoCuentaAlreadyExistsException.class, () -> cuentaService.darDeAltaCuenta(cuentaRepetida, cliente.getDni()));

        verify(clienteService, times(1)).agregarCuenta(cuentaRepetida, cliente.getDni());
        verify(cuentaDao, times(1)).save(cuenta);
        verify(cuentaDao, times(2)).find(cuenta.getNumeroCuenta());
    }

    //4. Cuenta creada exitosamente → debería verificarse que todas nuestras
    //   dependencias fueran invocadas exitosamente.
    @Test
    public void testCuentaCreadaSuccess() throws TipoCuentaAlreadyExistsException, CuentaAlreadyExistsException, TipoCuentaNoSoportadaException {
        Cliente cliente = getCliente(123456789L, "Pepo");
        Cuenta cuenta = getCuenta(cliente, TipoMoneda.PESOS, TipoCuenta.CUENTA_CORRIENTE);

        cuentaService.darDeAltaCuenta(cuenta, cliente.getDni());

        verify(clienteService, times(1)).agregarCuenta(cuenta, cliente.getDni());
        verify(cuentaDao, times(1)).save(cuenta);
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
