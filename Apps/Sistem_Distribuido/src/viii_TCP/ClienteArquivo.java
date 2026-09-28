package viii_TCP;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

//Aplicacao exemplo: pede um JPG ao ServidorArquivo e mostra-o num JLabel
public class ClienteArquivo extends JFrame {
	
	private JTextField txtServidor = new JTextField("localhost");
	private JTextField txtPorta = new JTextField("5001");
	private JTextField txtArquivo = new JTextField();
	private JTextField txtSaida = new JTextField();
	private JButton btnBuscar = new JButton("Buscar");
	private JLabel lblImagem = new JLabel("Sem imagem", JLabel.CENTER);

	public ClienteArquivo() {
		super("Cliente de arquivos TCP");
		JPanel form = new JPanel(new GridLayout(5, 2));
		form.add(new JLabel("Servidor:"));  form.add(txtServidor);
		form.add(new JLabel("Porta:"));     form.add(txtPorta);
		form.add(new JLabel("Arquivo no servidor:")); form.add(txtArquivo);
		form.add(new JLabel("Guardar como:"));        form.add(txtSaida);
		form.add(new JLabel());             form.add(btnBuscar);

		btnBuscar.addActionListener(this::btnBuscarActionPerformed);

		add(form, BorderLayout.NORTH);
		add(new JScrollPane(lblImagem), BorderLayout.CENTER);
		setSize(640, 520);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			//Cria o Socket para buscar o arquivo no servidor
			Socket rec = new Socket(txtServidor.getText(), Integer.parseInt(txtPorta.getText()));

			//Enviando o nome do arquivo a ser baixado do servidor
			ObjectOutputStream saida = new ObjectOutputStream(rec.getOutputStream());
			saida.writeObject(txtArquivo.getText());

			//DataInputStream para processar os bytes recebidos
			DataInputStream entrada = new DataInputStream(rec.getInputStream());
			//FileOutputStream para salvar o arquivo recebido
			FileOutputStream sarq = new FileOutputStream(txtSaida.getText());
			byte[] br = new byte[512];
			int leitura = entrada.read(br);
			while(leitura != -1) {
				if(leitura != -2) {
					sarq.write(br, 0, leitura);
				}
				leitura = entrada.read(br);
			}

			saida.close();
			entrada.close();
			sarq.close();
			rec.close();
			ImageIcon img = new ImageIcon(txtSaida.getText());
			lblImagem.setText("");
			lblImagem.setIcon(img);
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Exceção:" + e.getMessage(), "Erro", 2);
		}
	}

	void main() {
		new ClienteArquivo().setVisible(true);
		
	}
}
