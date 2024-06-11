package ar.edu.utn.frbb.tup.service;

import ar.edu.utn.frbb.tup.model.*;
import ar.edu.utn.frbb.tup.model.exception.ClienteAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.persistence.ClienteDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClienteServiceTest {

    @Mock
    private ClienteDao clienteDao;

    @InjectMocks
    private ClienteService clienteService;

    @BeforeAll
    public void setUp() {

    }

    @Test
    public void testClienteMenor18Años() {
        Cliente clienteMenorDeEdad = new Cliente();
        clienteMenorDeEdad.setFechaNacimiento(LocalDate.of(2020, 2, 7));
        assertThrows(IllegalArgumentException.class, () -> clienteService.darDeAltaCliente(clienteMenorDeEdad));
    }

    @Test
    public void testClienteSuccess() throws ClienteAlreadyExistsException {
        Cliente cliente = new Cliente();
        cliente.setFechaNacimiento(LocalDate.of(1978,3,25));
        cliente.setDni(29857643);
        cliente.setTipoPersona(TipoPersona.PERSONA_FISICA);
        clienteService.darDeAltaCliente(cliente);

        verify(clienteDao, times(1)).save(cliente);
    }

    @Test
    public void testClienteAlreadyExistsException() throws ClienteAlreadyExistsException {
        Cliente pepeRino = new Cliente();
        pepeRino.setDni(26456437);
        pepeRino.setNombre("Pepe");
        pepeRino.setApellido("Rino");
        pepeRino.setFechaNacimiento(LocalDate.of(1978, 3,25));
        pepeRino.setTipoPersona(TipoPersona.PERSONA_FISICA);

        when(clienteDao.find(26456437, false)).thenReturn(new Cliente());

        assertThrows(ClienteAlreadyExistsException.class, () -> clienteService.darDeAltaCliente(pepeRino));
    }

    @Test
    public void testAgregarCuentaAClienteSuccess() throws TipoCuentaAlreadyExistsException {
        Cliente pepeRino = new Cliente();
        pepeRino.setDni(26456439);
        pepeRino.setNombre("Pepe");
        pepeRino.setApellido("Rino");
        pepeRino.setFechaNacimiento(LocalDate.of(1978, 3,25));
        pepeRino.setTipoPersona(TipoPersona.PERSONA_FISICA);

        Cuenta cuenta = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(500000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        when(clienteDao.find(26456439, true)).thenReturn(pepeRino);

        clienteService.agregarCuenta(cuenta, pepeRino.getDni());

        verify(clienteDao, times(1)).save(pepeRino);

        assertEquals(1, pepeRino.getCuentas().size());
        assertEquals(pepeRino, cuenta.getTitular());

    }

    @Test
    public void testAgregarCuentaAClienteDuplicada() throws TipoCuentaAlreadyExistsException {
        Cliente luciano = new Cliente();
        luciano.setDni(26456439);
        luciano.setNombre("Pepe");
        luciano.setApellido("Rino");
        luciano.setFechaNacimiento(LocalDate.of(1978, 3,25));
        luciano.setTipoPersona(TipoPersona.PERSONA_FISICA);

        Cuenta cuenta = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(500000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        when(clienteDao.find(26456439, true)).thenReturn(luciano);

        clienteService.agregarCuenta(cuenta, luciano.getDni());

        Cuenta cuenta2 = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(500000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        assertThrows(TipoCuentaAlreadyExistsException.class, () -> clienteService.agregarCuenta(cuenta2, luciano.getDni()));
        verify(clienteDao, times(1)).save(luciano);
        assertEquals(1, luciano.getCuentas().size());
        assertEquals(luciano, cuenta.getTitular());

    }

    //1. Agregar una CA$ y CC$ → se puede agregar y se debe verificar que el cliente tenga
    //   2 cuentas, titular sea el cliente que se creó
    @Test
    public void testAgregarTiposCuentasSuccess() throws TipoCuentaAlreadyExistsException {
        Cliente peperino = getCliente(26456439, "Pepe");

        Cuenta cuentaCA = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(500000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        Cuenta cuentaCC = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(500000)
                .setTipoCuenta(TipoCuenta.CUENTA_CORRIENTE);

        when(clienteDao.find(26456439, true)).thenReturn(peperino);

        //Agrego las cuentas
        clienteService.agregarCuenta(cuentaCA, peperino.getDni());
        clienteService.agregarCuenta(cuentaCC, peperino.getDni());

        //Verifico si clienteDao.save fue usado 2 veces (ya que agregue dos cuentas)
        verify(clienteDao, times(2)).save(peperino);

        //Verifico que cliente tenga 2 cuentas
        assertEquals(2, peperino.getCuentas().size());
        //Verifico que el titular de la cuenta es el cliente que se creo
        assertEquals(peperino, cuentaCA.getTitular());
        assertEquals(peperino, cuentaCC.getTitular());
    }

    //2. Agregar una CA$ y CAU$S → se puede agregar y se debe verificar que el cliente
    //   tenga 2 cuentas, titular sea el cliente que se creó
    @Test
    public void testAgregarCuentasDiferentesDivisasSuccess() throws TipoCuentaAlreadyExistsException {
        Cliente peperino = getCliente(26456439, "Pepe");

        Cuenta cuentaCorrienteArs = new Cuenta()
                .setMoneda(TipoMoneda.PESOS)
                .setBalance(600000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);
        Cuenta cuentaCorrienteUsd = new Cuenta()
                .setMoneda(TipoMoneda.DOLARES)
                .setBalance(50000)
                .setTipoCuenta(TipoCuenta.CAJA_AHORRO);

        when(clienteDao.find(26456439, true)).thenReturn(peperino);

        //Agrego las cuentas
        clienteService.agregarCuenta(cuentaCorrienteArs, peperino.getDni());
        clienteService.agregarCuenta(cuentaCorrienteUsd, peperino.getDni());

        //Verifico si clienteDao.save fue usado 2 veces (ya que agregue dos cuentas
        verify(clienteDao, times(2)).save(peperino);

        //Verifico que cliente tenga 2 cuentas
        assertEquals(2, peperino.getCuentas().size());
        //Verifico que el titular de la cuenta es el cliente que se creo
        assertEquals(peperino, cuentaCorrienteArs.getTitular());
        assertEquals(peperino, cuentaCorrienteUsd.getTitular());
    }

    //3. Testear el método buscarPorDni (cómo mínimo son dos casos de test: casos de
    //   éxito y de falla)
    @Test
    public void testBuscarPorDniSuccess(){
        Cliente peperino = getCliente(123456789L, "Pepo");

        when(clienteDao.find(peperino.getDni(), true)).thenReturn(peperino);

        Cliente resultado = clienteService.buscarClientePorDni(peperino.getDni());

        assertNotNull(resultado);
        assertEquals(peperino, resultado);

        verify(clienteDao, times(1)).find(peperino.getDni(), true);

    }

    @Test
    public void testBuscarPorDniFail(){
        long dni = 1234566789L;

        when(clienteDao.find(dni, true)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> clienteService.buscarClientePorDni(dni));

        verify(clienteDao, times(1)).find(dni, true);
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
}