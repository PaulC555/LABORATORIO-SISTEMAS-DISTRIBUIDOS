package client;

import remote.CreditCardRemote;
import remote.TransactionRecord;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Cliente GUI para el servicio RMI de tarjetas de crédito.
 * Interfaz gráfica  usando Swing.
 */
public class CreditCardClientGUI extends JFrame {
    
    private static final String NOMBRE_SERVICIO = "CreditCardService";
    private static final int PUERTO_RMI = 1099;
    private static final String HOST = "localhost";
    
    // Colores 
    private static final Color COLOR_AZUL = new Color(25, 103, 210);
    private static final Color COLOR_AZUL_CLARO = new Color(63, 137, 240);
    private static final Color COLOR_VERDE = new Color(34, 177, 76);
    private static final Color COLOR_ROJO = new Color(229, 57, 53);
    private static final Color COLOR_FONDO = new Color(245, 246, 250);
    private static final Color COLOR_TEXTO = new Color(33, 33, 33);
    private static final Color COLOR_BORDE = new Color(224, 224, 224);
    
    private CreditCardRemote servicio;
    private JTabbedPane tabbedPane;
    
    /**
     * Constructor que inicializa la interfaz.
     */
    public CreditCardClientGUI() {
        setTitle("Sistema de Tarjetas de Credito");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setResizable(true);
        
        // Conectar al servicio RMI
        if (!conectarAlServicio()) {
            JOptionPane.showMessageDialog(this,
                "[ERROR] No se pudo conectar al servidor RMI\n\n" +
                "Verifique que:\n" +
                "1. RMI Registry esta activo en puerto 1098\n" +
                "2. El servidor esta ejecutandose\n\n"           ,
                "Error de Conexion",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // Crear interfaz
        inicializarUI();
        
        setVisible(true);
    }
    
    /**
     * Conecta al servicio RMI.
     */
    private boolean conectarAlServicio() {
        try {
            Registry registry = LocateRegistry.getRegistry(HOST, PUERTO_RMI);
            servicio = (CreditCardRemote) registry.lookup(NOMBRE_SERVICIO);
            return true;
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Inicializa la interfaz gráfica con pestañas.
     */
    private void inicializarUI() {
        // Panel superior con información
        JPanel panelSuperior = crearPanelSuperior();
        add(panelSuperior, BorderLayout.NORTH);
        
        // Panel con pestañas (sin historial)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBackground(COLOR_FONDO);
        tabbedPane.setForeground(COLOR_TEXTO);
        
        tabbedPane.addTab("[+] Crear Cuenta", crearPanelCrearCuenta());
        tabbedPane.addTab("[Cargo]", crearPanelRealizarCargo());
        tabbedPane.addTab("[Pago]", crearPanelRealizarPago());
        tabbedPane.addTab("[Saldo]", crearPanelConsultarSaldo());
        tabbedPane.addTab("[Info]", crearPanelInformacion());
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * Crea panel superior con información y estilo.
     */
    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_AZUL);
        panel.setForeground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel titulo = new JLabel("[BANCO] Sistema de Tarjetas de Credito");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titulo.setForeground(Color.WHITE);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(titulo);
        panel.add(Box.createVerticalStrut(8));
        
        return panel;
    }
    
    /**
     * Panel: Crear Nueva Cuenta
     */
    private JPanel crearPanelCrearCuenta() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Título
        JLabel titulo = new JLabel("[+] Nueva Cuenta de Credito");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(COLOR_AZUL);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titulo, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(Box.createVerticalStrut(15), gbc);
        
        // Número de cuenta
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel lblNumero = new JLabel("Número de Cuenta:");
        lblNumero.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNumero.setForeground(COLOR_TEXTO);
        panel.add(lblNumero, gbc);
        
        gbc.gridx = 1;
        JTextField tfNumeroCuenta = crearCampoTexto("4532-1234-5678-9010");
        panel.add(tfNumeroCuenta, gbc);
        
        // Nombre titular
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel lblNombre = new JLabel("Nombre del Titular:");
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNombre.setForeground(COLOR_TEXTO);
        panel.add(lblNombre, gbc);
        
        gbc.gridx = 1;
        JTextField tfNombre = crearCampoTexto("Juan Pérez");
        panel.add(tfNombre, gbc);
        
        // Límite crédito
        gbc.gridy = 4;
        gbc.gridx = 0;
        JLabel lblLimite = new JLabel("Límite de Crédito ($):");
        lblLimite.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLimite.setForeground(COLOR_TEXTO);
        panel.add(lblLimite, gbc);
        
        gbc.gridx = 1;
        JTextField tfLimite = crearCampoTexto("5000");
        panel.add(tfLimite, gbc);
        
        // Botón Crear
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 15, 12, 15);
        JButton btnCrear = crearBoton("[OK] Crear Cuenta", COLOR_VERDE);
        
        btnCrear.addActionListener(e -> {
            try {
                String numero = tfNumeroCuenta.getText().trim();
                String nombre = tfNombre.getText().trim();
                String limiteStr = tfLimite.getText().trim();
                
                if (numero.isEmpty() || nombre.isEmpty() || limiteStr.isEmpty()) {
                    mostrarError("Por favor complete todos los campos");
                    return;
                }
                
                double limite = Double.parseDouble(limiteStr);
                
                if (servicio.crearCuenta(numero, nombre, limite)) {
                    mostrarExito("[OK] Cuenta creada exitosamente\n\n" +
                        "Número: " + numero + "\n" +
                        "Titular: " + nombre + "\n" +
                        "Límite: $" + String.format("%.2f", limite));
                    
                    tfNumeroCuenta.setText("");
                    tfNombre.setText("");
                    tfLimite.setText("");
                }
            } catch (NumberFormatException ex) {
                mostrarError("El limite debe ser un numero valido");
            } catch (IllegalArgumentException ex) {
                mostrarError("[ERROR] " + ex.getMessage());
            } catch (RemoteException ex) {
                mostrarErrorRMI(ex);
            }
        });
        
        panel.add(btnCrear, gbc);
        
        // Espaciador
        gbc.gridy = 6;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    /**
     * Panel: Realizar Cargo (Compra)
     */
    private JPanel crearPanelRealizarCargo() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Título
        JLabel titulo = new JLabel("[Cargo] Realizar Compra");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(COLOR_ROJO);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titulo, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(Box.createVerticalStrut(15), gbc);
        
        // Número de cuenta
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(crearEtiqueta("Número de Cuenta:"), gbc);
        gbc.gridx = 1;
        JTextField tfNumeroCuenta = crearCampoTexto("");
        panel.add(tfNumeroCuenta, gbc);
        
        // Monto
        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(crearEtiqueta("Monto ($):"), gbc);
        gbc.gridx = 1;
        JTextField tfMonto = crearCampoTexto("150.50");
        panel.add(tfMonto, gbc);
        
        // Descripción
        gbc.gridy = 4;
        gbc.gridx = 0;
        panel.add(crearEtiqueta("Descripción:"), gbc);
        gbc.gridx = 1;
        JTextField tfDescripcion = crearCampoTexto("Compra en tienda");
        panel.add(tfDescripcion, gbc);
        
        // Botón Realizar Cargo
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 15, 12, 15);
        JButton btnCargo = crearBoton("[OK] Realizar Cargo", COLOR_ROJO);
        
        btnCargo.addActionListener(e -> {
            try {
                String numero = tfNumeroCuenta.getText().trim();
                String montoStr = tfMonto.getText().trim();
                String descripcion = tfDescripcion.getText().trim();
                
                if (numero.isEmpty() || montoStr.isEmpty()) {
                    mostrarError("Por favor complete número y monto");
                    return;
                }
                
                double monto = Double.parseDouble(montoStr);
                
                if (servicio.realizarCargo(numero, monto, descripcion)) {
                    double saldoActual = servicio.obtenerSaldo(numero);
                    
                    mostrarExito("[OK] Cargo realizado exitosamente\n\n" +
                        "Monto: $" + String.format("%.2f", monto) + "\n" +
                        "Saldo: $" + String.format("%.2f", saldoActual));
                    
                    tfNumeroCuenta.setText("");
                    tfMonto.setText("");
                    tfDescripcion.setText("");
                }
            } catch (NumberFormatException ex) {
                mostrarError("El monto debe ser un numero valido");
            } catch (IllegalArgumentException ex) {
                mostrarError("[ERROR] " + ex.getMessage());
            } catch (RemoteException ex) {
                mostrarErrorRMI(ex);
            }
        });
        
        panel.add(btnCargo, gbc);
        
        // Espaciador
        gbc.gridy = 6;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    /**
     * Panel: Realizar Pago
     */
    private JPanel crearPanelRealizarPago() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Título
        JLabel titulo = new JLabel("[Pago] Realizar Deposito");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(COLOR_VERDE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titulo, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(Box.createVerticalStrut(15), gbc);
        
        // Número de cuenta
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(crearEtiqueta("Número de Cuenta:"), gbc);
        gbc.gridx = 1;
        JTextField tfNumeroCuenta = crearCampoTexto("");
        panel.add(tfNumeroCuenta, gbc);
        
        // Monto a pagar
        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(crearEtiqueta("Monto a Pagar ($):"), gbc);
        gbc.gridx = 1;
        JTextField tfMonto = crearCampoTexto("100.00");
        panel.add(tfMonto, gbc);
        
        // Descripción
        gbc.gridy = 4;
        gbc.gridx = 0;
        panel.add(crearEtiqueta("Descripción:"), gbc);
        gbc.gridx = 1;
        JTextField tfDescripcion = crearCampoTexto("Pago en línea");
        panel.add(tfDescripcion, gbc);
        
        // Botón Realizar Pago
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 15, 12, 15);
        JButton btnPago = crearBoton("[OK] Realizar Pago", COLOR_VERDE);
        
        btnPago.addActionListener(e -> {
            try {
                String numero = tfNumeroCuenta.getText().trim();
                String montoStr = tfMonto.getText().trim();
                String descripcion = tfDescripcion.getText().trim();
                
                if (numero.isEmpty() || montoStr.isEmpty()) {
                    mostrarError("Por favor complete número y monto");
                    return;
                }
                
                double monto = Double.parseDouble(montoStr);
                
                if (servicio.realizarPago(numero, monto, descripcion)) {
                    double saldoActual = servicio.obtenerSaldo(numero);
                    
                    mostrarExito("[OK] Pago realizado exitosamente\n\n" +
                        "Monto pagado: $" + String.format("%.2f", monto) + "\n" +
                        "Nuevo saldo: $" + String.format("%.2f", saldoActual));
                    
                    tfNumeroCuenta.setText("");
                    tfMonto.setText("");
                    tfDescripcion.setText("");
                }
            } catch (NumberFormatException ex) {
                mostrarError("El monto debe ser un numero valido");
            } catch (IllegalArgumentException ex) {
                mostrarError("[ERROR] " + ex.getMessage());
            } catch (RemoteException ex) {
                mostrarErrorRMI(ex);
            }
        });
        
        panel.add(btnPago, gbc);
        
        // Espaciador
        gbc.gridy = 6;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    /**
     * Panel: Consultar Saldo
     */
    private JPanel crearPanelConsultarSaldo() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Título
        JLabel titulo = new JLabel("[Saldo] Consultar Disponible");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(COLOR_AZUL);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titulo, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(Box.createVerticalStrut(15), gbc);
        
        // Número de cuenta
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(crearEtiqueta("Número de Cuenta:"), gbc);
        gbc.gridx = 1;
        JTextField tfNumeroCuenta = crearCampoTexto("");
        panel.add(tfNumeroCuenta, gbc);
        
        // Área de resultados
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 15, 12, 15);
        
        JTextArea taResultado = new JTextArea(10, 40);
        taResultado.setFont(new Font("Courier New", Font.PLAIN, 13));
        taResultado.setBackground(Color.WHITE);
        taResultado.setForeground(COLOR_TEXTO);
        taResultado.setBorder(new LineBorder(COLOR_BORDE, 2));
        taResultado.setEditable(false);
        taResultado.setLineWrap(true);
        taResultado.setWrapStyleWord(true);
        taResultado.setMargin(new Insets(15, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(taResultado);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, gbc);
        
        // Botón Consultar
        gbc.gridy = 4;
        gbc.insets = new Insets(12, 15, 12, 15);
        JButton btnConsultar = crearBoton("[OK] Consultar Saldo", COLOR_AZUL);
        
        btnConsultar.addActionListener(e -> {
            try {
                String numero = tfNumeroCuenta.getText().trim();
                
                if (numero.isEmpty()) {
                    mostrarError("Por favor ingrese el número de cuenta");
                    return;
                }
                
                // Obtener información de la cuenta
                String infoCompleta = servicio.obtenerInfoCuenta(numero);
                
                // Parsear la información para mostrar coherentemente
                // Formato esperado: número | nombre | saldo | límite | deuda
                String[] partes = infoCompleta.split("\\|");
                
                if (partes.length >= 4) {
                    String numeroCta = partes[0].trim();
                    String nombre = partes[1].trim();
                    double saldo = Double.parseDouble(partes[2].trim());
                    double limite = Double.parseDouble(partes[3].trim());
                    double deuda = Math.abs(saldo);  // Deuda es valor absoluto del saldo negativo
                    double disponible = limite - deuda;  // Disponible = límite - deuda
                    
                    String estado;
                    Color color;
                    if (disponible > 0) {
                        estado = "CON CRÉDITO DISPONIBLE";
                        color = COLOR_VERDE;
                    } else if (disponible == 0) {
                        estado = "LÍMITE ALCANZADO";
                        color = new Color(255, 152, 0);  // Naranja
                    } else {
                        estado = "SOBREGIRO";
                        color = COLOR_ROJO;
                    }
                    
                    String resultado = String.format(
                        "═══════════════════════════════════════════\n" +
                        "           INFORMACIÓN DE CUENTA\n" +
                        "═══════════════════════════════════════════\n\n" +
                        "Número de Cuenta:     %s\n" +
                        "Titular:              %s\n\n" +
                        "Límite de Crédito:    $%-8.2f\n" +
                        "Deuda Actual:         $%-8.2f\n" +
                        "Saldo Disponible:     $%-8.2f\n\n" +
                        "Estado: %s\n" +
                        "═══════════════════════════════════════════",
                        numeroCta, nombre, limite, deuda, disponible, estado
                    );
                    
                    taResultado.setText(resultado);
                    taResultado.setForeground(color);
                }
                
            } catch (IllegalArgumentException ex) {
                mostrarError("Error: " + ex.getMessage());
                taResultado.setText("");
            } catch (RemoteException ex) {
                mostrarErrorRMI(ex);
                taResultado.setText("");
            }
        });
        
        panel.add(btnConsultar, gbc);
        
        // Espaciador
        gbc.gridy = 5;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    /**
     * Panel: Información de Cuenta
     */
    private JPanel crearPanelInformacion() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Título
        JLabel titulo = new JLabel("[Info] Detalles de Cuenta");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(COLOR_AZUL);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titulo, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(Box.createVerticalStrut(15), gbc);
        
        // Número de cuenta
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(crearEtiqueta("Número de Cuenta:"), gbc);
        gbc.gridx = 1;
        JTextField tfNumeroCuenta = crearCampoTexto("");
        panel.add(tfNumeroCuenta, gbc);
        
        // Área de resultados
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 15, 12, 15);
        
        JTextArea taResultado = new JTextArea(10, 40);
        taResultado.setFont(new Font("Courier New", Font.PLAIN, 12));
        taResultado.setBackground(Color.WHITE);
        taResultado.setForeground(COLOR_TEXTO);
        taResultado.setBorder(new LineBorder(COLOR_BORDE, 2));
        taResultado.setEditable(false);
        taResultado.setLineWrap(true);
        taResultado.setWrapStyleWord(true);
        taResultado.setMargin(new Insets(15, 15, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(taResultado);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, gbc);
        
        // Botón Obtener Info
        gbc.gridy = 4;
        gbc.insets = new Insets(12, 15, 12, 15);
        JButton btnInfo = crearBoton("[OK] Obtener Informacion", COLOR_AZUL);
        
        btnInfo.addActionListener(e -> {
            try {
                String numero = tfNumeroCuenta.getText().trim();
                
                if (numero.isEmpty()) {
                    mostrarError("Por favor ingrese el número de cuenta");
                    return;
                }
                
                String info = servicio.obtenerInfoCuenta(numero);
                taResultado.setText(info);
                
            } catch (IllegalArgumentException ex) {
                mostrarError("❌ Error: " + ex.getMessage());
                taResultado.setText("");
            } catch (RemoteException ex) {
                mostrarErrorRMI(ex);
                taResultado.setText("");
            }
        });
        
        panel.add(btnInfo, gbc);
        
        // Espaciador
        gbc.gridy = 5;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        return panel;
    }
    
    /**
     * Crea un campo de texto estilizado.
     */
    private JTextField crearCampoTexto(String placeholder) {
        JTextField tf = new JTextField(25);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setBorder(new LineBorder(COLOR_BORDE, 2));
        tf.setMargin(new Insets(8, 10, 8, 10));
        tf.setBackground(Color.WHITE);
        tf.setForeground(COLOR_TEXTO);
        tf.setText(placeholder);
        
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder) && !placeholder.isEmpty()) {
                    tf.setText("");
                    tf.setForeground(COLOR_TEXTO);
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty() && !placeholder.isEmpty()) {
                    tf.setForeground(new Color(170, 170, 170));
                    tf.setText(placeholder);
                }
            }
        });
        
        return tf;
    }
    
    /**
     * Crea una etiqueta estilizada.
     */
    private JLabel crearEtiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(COLOR_TEXTO);
        return lbl;
    }
    
    /**
     * Crea un botón estilizado.
     */
    private JButton crearBoton(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(color.darker(), 2, true));
        btn.setMargin(new Insets(10, 20, 10, 20));
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        
        // Efecto hover
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        
        return btn;
    }
    
    /**
     * Muestra un diálogo de error.
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Muestra un diálogo de éxito.
     */
    private void mostrarExito(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Éxito", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Muestra un diálogo de error RMI.
     */
    private void mostrarErrorRMI(Exception ex) {
        JOptionPane.showMessageDialog(this,
            "Error de conexión RMI:\n\n" + ex.getMessage(),
            "Error RMI",
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Punto de entrada de la aplicación.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new CreditCardClientGUI();
        });
    }
}
