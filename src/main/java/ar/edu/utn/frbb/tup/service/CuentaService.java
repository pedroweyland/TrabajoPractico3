package ar.edu.utn.frbb.tup.service;

import ar.edu.utn.frbb.tup.model.Cuenta;
import ar.edu.utn.frbb.tup.model.TipoCuenta;
import ar.edu.utn.frbb.tup.model.TipoMoneda;
import ar.edu.utn.frbb.tup.model.exception.CuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaAlreadyExistsException;
import ar.edu.utn.frbb.tup.model.exception.TipoCuentaNoSoportadaException;
import ar.edu.utn.frbb.tup.persistence.CuentaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CuentaService {

    @Autowired
    CuentaDao cuentaDao;

    @Autowired
    ClienteService clienteService;

    //Generar casos de test para darDeAltaCuenta
    //    1 - cuenta existente
    //    2 - cuenta no soportada
    //    3 - cliente ya tiene cuenta de ese tipo
    //    4 - cuenta creada exitosamente
    public void darDeAltaCuenta(Cuenta cuenta, long dniTitular) throws CuentaAlreadyExistsException, TipoCuentaAlreadyExistsException, TipoCuentaNoSoportadaException {
        if(cuentaDao.find(cuenta.getNumeroCuenta()) != null) {
            throw new CuentaAlreadyExistsException("La cuenta " + cuenta.getNumeroCuenta() + " ya existe.");
        }

        //Chequear cuentas soportadas por el banco CA$ CC$ CAU$S
        if (!tipoCuentaEstaSoportada(cuenta)) {
            throw new TipoCuentaNoSoportadaException("El tipo de cuenta no es soportada, solo se admite CA$ - CC$ - CAU$S");
        }

        clienteService.agregarCuenta(cuenta, dniTitular);
        cuentaDao.save(cuenta);
    }

    public boolean tipoCuentaEstaSoportada(Cuenta cuenta){

        if (cuenta.getMoneda() == TipoMoneda.DOLARES && cuenta.getTipoCuenta() == TipoCuenta.CAJA_AHORRO){
            return true; //Puede soportar una CAJA_AHORRO en dolares
        }

        if (cuenta.getMoneda() == TipoMoneda.PESOS && (cuenta.getTipoCuenta() == TipoCuenta.CUENTA_CORRIENTE || cuenta.getTipoCuenta() == TipoCuenta.CAJA_AHORRO)){
            return true; //Puede soportar una CUENTA_CORRIENTE o CAJA_AHORRO en PESOS
        }

        return false; //No soportaria una CUENTA_CORRIENTE en dolares
    }

    public Cuenta find(long id) {
        return cuentaDao.find(id);
    }
}
