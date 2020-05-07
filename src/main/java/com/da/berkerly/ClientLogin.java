package com.da.berkerly;

/**在本类109行附近调用了ChatClient类
 *
 */

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;


public class ClientLogin extends JFrame  {
    private JTextField nametext  ;
    private JPasswordField passwordtetx ;
    //private Object bPanel;

    public ClientLogin()  {
        this.init() ;       //init方法初始化
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    public void init()  {
        this.setTitle("Chatting room");
        this.setSize(300,150);     //借用成熟美观尺寸
        int y = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight() ;
        int x = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth() ;
        /**
         *
         */
        this.setLocation( (x-this.getWidth() )/2, ( y-this.getHeight() )/2 );
        this.setResizable(false);     //不允许用户自行更改大小
        Icon icon = new ImageIcon("d:"+File.separator+"login.jpg") ;
        JLabel label = new JLabel(icon) ;   //设置登陆界面上边框
        this.add(label,BorderLayout.NORTH) ;

        JPanel mainPanel = new JPanel() ;
        Border border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED) ;
        mainPanel.setBorder(BorderFactory.createTitledBorder(border,"Please input your username",TitledBorder.CENTER,TitledBorder.TOP)) ;
        this.add(mainPanel,BorderLayout.CENTER) ;     //将主面板加入frame
        mainPanel.setLayout(null) ;
        JLabel namelabel = new JLabel("Username") ;
        namelabel.setBounds(30,30,80,22) ;
        mainPanel.add(namelabel) ;
        nametext = new JTextField() ;
        nametext.setBounds(115,30,120,22);
        mainPanel.add(nametext) ;
        /*JLabel passwordlabel = new JLabel("please enter") ;
        passwordlabel.setBounds(30,60,80,22);
        mainPanel.add(passwordlabel) ;
        passwordtetx = new JPasswordField() ;
        passwordtetx.setBounds(115,60,120,22) ;
        mainPanel.add(passwordtetx) ;*/

        //接下来按钮位置排放
        JPanel bPanel = new JPanel() ;
        bPanel.setLayout(new FlowLayout(FlowLayout.RIGHT)) ;
        this.add(bPanel,BorderLayout.SOUTH) ;
        JButton reset = new JButton("reset") ;
        reset.addActionListener(new ActionListener()  {    //为“重置”按钮添加事件监听
            public void actionPerformed(ActionEvent e)  {
                nametext.setText("");
                passwordtetx.setText("");
            }
        });
        bPanel.add(reset) ;

        /**下面开始实现提交按钮
         *
         */

        JButton submit = new JButton("login") ;
        submit.addActionListener(new LoginAction(this) );  //因为登陆相对复杂，重新为登陆写一个类
        bPanel.add(submit) ;
    }



    /**下面开始写登陆类
     *
     */

    class LoginAction implements ActionListener  {
        private JFrame self ;
        public LoginAction(JFrame self)  {
            this.self = self ;
        }

        public void actionPerformed(ActionEvent e)  {
            //System.out.println("Username："+nametext.getText()+" Password："+new String(passwordtext.getPassword())) ;
            try  {
                //开始连接到服务器
                //Socket socket = new Socket("127.0.0.1",8090) ;
                Socket socket = new Socket("35.189.32.209",8090) ;
                new ChatClient(socket,nametext.getText()) ;
                //调用dispose方法关闭登陆框
                self.dispose();
            }catch(UnknownHostException e1)  {
                e1.printStackTrace();
                JOptionPane.showConfirmDialog(self, "Can not find the server","Connection fail",JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE) ;
            }catch(IOException e1)  {
                e1.printStackTrace() ;
                JOptionPane.showConfirmDialog(self, "Can not find the server","Connection fail",JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE) ;
            }
        }
    }
    public static void main(String args[])  {
        new ClientLogin() ;
    }
}
