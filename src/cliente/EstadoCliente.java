package cliente;
import objetos_comuns.Cargo; import objetos_comuns.CorCarta;

public class EstadoCliente {
    private volatile Cargo cargo;
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public boolean temCargo() { return cargo != null; }
    public boolean ehMestre() { return cargo != null && cargo.eMestreEspiao(); }
    public CorCarta getTime() { return cargo == null ? null : cargo.time(); }
}
