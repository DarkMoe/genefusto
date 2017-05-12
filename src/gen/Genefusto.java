package gen;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import gen.addressing.AbsoluteLong;
import gen.addressing.AbsoluteShort;
import gen.addressing.AddressRegisterDirect;
import gen.addressing.AddressRegisterIndirect;
import gen.addressing.AddressRegisterIndirectPostIncrement;
import gen.addressing.AddressRegisterIndirectPreDecrement;
import gen.addressing.AddressRegisterWithDisplacement;
import gen.addressing.AddressRegisterWithIndex;
import gen.addressing.AddressingMode;
import gen.addressing.DataRegisterDirect;
import gen.addressing.ImmediateData;
import gen.addressing.PCWithDisplacement;
import gen.addressing.PCWithIndex;
import gen.instruction.ABCD;
import gen.instruction.ADD;
import gen.instruction.ADDA;
import gen.instruction.ADDI;
import gen.instruction.ADDQ;
import gen.instruction.ADDX;
import gen.instruction.AND;
import gen.instruction.ANDI;
import gen.instruction.ANDI_CCR;
import gen.instruction.ANDI_SR;
import gen.instruction.ASL;
import gen.instruction.ASR;
import gen.instruction.BCC;
import gen.instruction.BCHG;
import gen.instruction.BCLR;
import gen.instruction.BSET;
import gen.instruction.BTST;
import gen.instruction.CLR;
import gen.instruction.CMP;
import gen.instruction.CMPA;
import gen.instruction.CMPI;
import gen.instruction.CMPM;
import gen.instruction.DBcc;
import gen.instruction.DIVS;
import gen.instruction.DIVU;
import gen.instruction.EOR;
import gen.instruction.EORI;
import gen.instruction.EXG;
import gen.instruction.EXT;
import gen.instruction.JMP;
import gen.instruction.JSR;
import gen.instruction.LEA;
import gen.instruction.LINK;
import gen.instruction.LSL;
import gen.instruction.LSR;
import gen.instruction.MOVE;
import gen.instruction.MOVEA;
import gen.instruction.MOVEM;
import gen.instruction.MOVEP;
import gen.instruction.MOVEQ;
import gen.instruction.MOVE_FROM_SR;
import gen.instruction.MOVE_TO_CCR;
import gen.instruction.MOVE_TO_FROM_USP;
import gen.instruction.MOVE_TO_SR;
import gen.instruction.MULS;
import gen.instruction.MULU;
import gen.instruction.NEG;
import gen.instruction.NOP;
import gen.instruction.NOT;
import gen.instruction.OR;
import gen.instruction.ORI;
import gen.instruction.ORI_CCR;
import gen.instruction.ORI_SR;
import gen.instruction.PEA;
import gen.instruction.ROR;
import gen.instruction.ROXL;
import gen.instruction.ROXR;
import gen.instruction.RTE;
import gen.instruction.RTS;
import gen.instruction.SBCD;
import gen.instruction.SUB;
import gen.instruction.SUBA;
import gen.instruction.SUBI;
import gen.instruction.SUBQ;
import gen.instruction.SWAP;
import gen.instruction.Scc;
import gen.instruction.TRAP;
import gen.instruction.TST;
import gen.instruction.UNLK;

//	MEMORY MAP:	https://en.wikibooks.org/wiki/Genesis_Programming

public class Genefusto {
	
    GenMemory memory;
    GenVdp vdp;
    GenBus bus;
    GenZ80 z80;
    Gen68 cpu;
    GenJoypad joypad;
    
    private static int[] pixels;

    int debugMemoryChangedAddress;
    int debugMemoryChangedData;
    
    boolean romCargo = false;

    int CLOCKSPEED = 4194304;
    
    final JFrame jframe = new JFrame("GeNEFUSTO");
    private Thread currentGameThread;
    private MyRunnable currentRunna;
    private boolean isRomOpened;
    
    StringBuilder lineLog = new StringBuilder(300);
    
    static BufferedImage img = new BufferedImage(320, 256, BufferedImage.TYPE_INT_RGB);
    
    public static void main(String[] args) throws Exception {
        // Create the frame on the event dispatching thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Genefusto();
            }
        });
    }
    
    Genefusto() {
        this(false);
    }
    
    final JLabel label = new JLabel(new ImageIcon(img));

    Genefusto(boolean debug) {
    	bus = new GenBus(this, null, null, null, null, null);
        
    	memory = new GenMemory();
        vdp = new GenVdp(bus);
        z80 = new GenZ80(bus);
        cpu = new Gen68(bus);
        joypad = new GenJoypad();
        
        bus.memory = memory;
        bus.vdp = vdp;
        bus.z80 = z80;
        bus.joypad = joypad;
        bus.cpu = cpu;

        new ABCD(cpu).generate();
        new ADD(cpu).generate();
        new ADDA(cpu).generate();
        new ADDI(cpu).generate();
        new ADDQ(cpu).generate();
        new ADDX(cpu).generate();
        new AND(cpu).generate();
        new ANDI(cpu).generate();
        new ANDI_CCR(cpu).generate();
        new ANDI_SR(cpu).generate();
        new ASL(cpu).generate();
        new ASR(cpu).generate();
        new BCC(cpu).generate();
        new BCHG(cpu).generate();
        new BCLR(cpu).generate();
        new BSET(cpu).generate();
        new BTST(cpu).generate();
        new CLR(cpu).generate();
        new CMP(cpu).generate();
        new CMPA(cpu).generate();
        new CMPI(cpu).generate();
        new CMPM(cpu).generate();
        new DBcc(cpu).generate();
        new DIVS(cpu).generate();
        new DIVU(cpu).generate();
        new EOR(cpu).generate();
        new EORI(cpu).generate();
        new EXG(cpu).generate();
        new EXT(cpu).generate();
        new JMP(cpu).generate();
        new JSR(cpu).generate();
        new LEA(cpu).generate();
        new LINK(cpu).generate();
        new LSL(cpu).generate();
        new LSR(cpu).generate();
        new MOVE(cpu).generate();
        new MOVEA(cpu).generate();
        new MOVE_FROM_SR(cpu).generate();
        new MOVE_TO_CCR(cpu).generate();
        new MOVE_TO_SR(cpu).generate();
        new MOVE_TO_FROM_USP(cpu).generate();
        new MOVEM(cpu).generate();
        new MOVEP(cpu).generate();
        new MOVEQ(cpu).generate();
        new MULS(cpu).generate();
        new MULU(cpu).generate();
        new NEG(cpu).generate();
        new NOP(cpu).generate();
        new NOT(cpu).generate();
        new OR(cpu).generate();
        new ORI(cpu).generate();
        new ORI_CCR(cpu).generate();
        new ORI_SR(cpu).generate();
        new PEA(cpu).generate();
        new ROR(cpu).generate();
        new ROXL(cpu).generate();
        new ROXR(cpu).generate();
        new RTE(cpu).generate();
        new RTS(cpu).generate();
        new SBCD(cpu).generate();
        new Scc(cpu).generate();
        new SUB(cpu).generate();
        new SUBA(cpu).generate();
        new SUBI(cpu).generate();
        new SUBQ(cpu).generate();
        new SWAP(cpu).generate();
        new TRAP(cpu).generate();
        new TST(cpu).generate();
        new UNLK(cpu).generate();
        
        System.out.println(cpu.totalInstructions);
        
		cpu.addressingModes = new AddressingMode[] {
			new DataRegisterDirect(cpu),
			new AddressRegisterDirect(cpu),
			new AddressRegisterIndirect(cpu),
			new AddressRegisterIndirectPostIncrement(cpu),
			new AddressRegisterIndirectPreDecrement(cpu),
			new AddressRegisterWithDisplacement(cpu),
			new AddressRegisterWithIndex(cpu),
			
			new AbsoluteShort(cpu),
			new AbsoluteLong(cpu),
			new PCWithDisplacement(cpu),
			new PCWithIndex(cpu),
			new ImmediateData(cpu),	//	solo si es un source operand TODO, si es writting es StatusRegisterOperand
		};
		
    	try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { }
        
        pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        
        Graphics g = img.getGraphics();
        g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
        g.dispose();

        JMenuBar bar = new JMenuBar();
    
        JMenu menu = new JMenu("File");
        bar.add(menu);
//        JMenu viewMenu = new JMenu("View");
//        bar.add(viewMenu);
        JMenu helpMenu = new JMenu("Nefusto");
        bar.add(helpMenu);
        
        JMenuItem loadRomItem = new JMenuItem("Load ROM");
        loadRomItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openRomDialog();
            }
        });
        JMenuItem closeRomItem = new JMenuItem("Close ROM");
        closeRomItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                markForClose = true;
            }
        });
        JMenuItem loadItem = new JMenuItem("Quick load");
        loadItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                markForLoad = true;
            }
        });
        JMenuItem saveItem = new JMenuItem("Quick save");
        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                markForSave = true;
            }
        });
        
//        for (int i = 1; i <= 4; i++) {
//        	JMenuItem zoomItem = new JMenuItem("x" + i);
//        	zoomItem.addActionListener(new ScreenListener(i));
//        	viewMenu.add(zoomItem);
//		}
        
        JCheckBoxMenuItem biosItem = new JCheckBoxMenuItem("Use BIOS", true);
        biosItem.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.DESELECTED) {
//            		loadBios = false;
            	} else {
//            		loadBios = true;
            	}
			}
        });
//        viewMenu.add(biosItem);
        
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Nefusto, Nefusto... Barril sin fondo");
            }
        });
        
        menu.add(loadRomItem);
        menu.add(closeRomItem);
        menu.add(loadItem);
        menu.add(saveItem);
        
        helpMenu.add(aboutItem);
        
        jframe.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keyPressedHandler(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keyReleasedHandler(e);
            }
        });
        
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.setLocation(400, 400);
        jframe.setResizable(false);
        jframe.setJMenuBar(bar);
        jframe.add(label);
        jframe.pack();
        jframe.setVisible(true);
    }

    public void printLog(String fullOpcode) {
//    	if (cpu.halted) {
//    		System.out.println("HALTED");
//    	} else {
//	    	lineLog.setLength(0);
//	        lineLog.append("af: ").append(cpu.pad(cpu.A)).append(cpu.pad(cpu.F)).append(" - bc: ").append(cpu.pad(cpu.B))
//	            .append(cpu.pad(cpu.C)).append(" - de: ").append(cpu.pad(cpu.D)).append(cpu.pad(cpu.E)).append(" - hl: ")
//	            .append(cpu.pad(cpu.H)).append(cpu.pad(cpu.L)).append("\npc: ").append(cpu.pad4(cpu.programCounter - 1))
//	            .append(" - sp: ").append(cpu.pad4(cpu.stackPointer)).append(" - opcode: ").append(fullOpcode)
//	            .append("\n").append("lcdc: ").append(cpu.pad(vdp.lcdControl)).append(" - stat: ")
//	            .append(cpu.pad(vdp.ff41)).append(" - ly: ").append(cpu.pad(vdp.ff44)).append(" - IE: ")
//	            .append(cpu.pad(cpu.ffff)).append(" - IF: ").append(cpu.pad(cpu.ff0f)).append("\n")
//	            .append("04: ").append(cpu.pad(timer.ff04)).append(" - 05: ").append(cpu.pad(timer.ff05))
//	            .append(" - 06: ").append(cpu.pad(timer.ff06)).append(" - 07: ").append(cpu.pad(timer.ff07))
//	            .append("\n").append("scanline: ").append(vdp.scanlineCounter).append(" - RomBank: ").append(cpu.pad(memory.currentROMBank))
//	            .append(" - Vram Bank: ").append(cpu.pad(vdp.currentVramBank))
//	            .append(" - Wram Bank: ").append(cpu.pad(memory.currentWramBank)).append("\n");
//	//        	.append("BGPal: ").append(pad(rom[0xFF68])).append(" - BGPal data: ").append(pad(rom[0xFF69]))
//	//        	.append(" - OBJPal: ").append(pad(rom[0xFF6A])).append(" - OBJPal data: ").append(pad(rom[0xFF6B])).append("\n");
//	        System.out.println(lineLog.toString());
//    	}
    }
    
    class ScreenListener implements ActionListener {
        
    	int multiplier;
    	
    	public ScreenListener(int multi) {
    		multiplier = multi;
    	}
    	
    	@Override
        public void actionPerformed(ActionEvent e) {
        	adjustScreen();
        }

		private void adjustScreen() {
			int multi = multiplier;
			currentMultiplier = multi;
			
			jframe.setSize(160 * multi, 144 * multi);
            img = new BufferedImage(160 * multi, 144 * multi, BufferedImage.TYPE_INT_RGB);
            pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            
            Graphics g = img.getGraphics();
            g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
            g.dispose();

            jframe.remove(label);
            final JLabel newLabel = new JLabel(new ImageIcon(img));
            jframe.add(newLabel);
            
            jframe.pack();
		}
    }
    
    private void openRomDialog() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "md files";
            }
            @Override
            public boolean accept(File f) {
                String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".md");
            }
        });
        fileChooser.setCurrentDirectory(new File(basePath));
        int result = fileChooser.showOpenDialog(jframe);
        if (result == JFileChooser.APPROVE_OPTION) {
            if (isRomOpened) {
//                markForClose = true;
                try {
                    currentGameThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isRomOpened = true;
            File selectedFile = fileChooser.getSelectedFile();
            currentRunna = new MyRunnable(selectedFile);
            currentGameThread = new Thread(currentRunna);
            currentGameThread.start();
        }
    }

    String basePath = "C:\\Users\\Zotac\\workspace\\raul\\src\\gen\\roms\\";
//	String basePath = "C:\\dev\\workspace\\test\\src\\gen\\roms\\";
    
    class MyRunnable implements Runnable {
        File file;
        
        public MyRunnable(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            if (file.getName().toLowerCase().endsWith(".zip")) {
//                memory.cartridgeMemory = GBFileLoader.readZipFile(file);
            } else if (file.getName().toLowerCase().endsWith(".md")) {
                memory.cartridge = FileLoader.readFile(file);
            }
        
            String rom = file.getName();
            jframe.setTitle(jframe.getTitle() + " - " + rom);
            
            cpu.reset();
            cpu.initialize();
            joypad.initialize();
            vdp.init();
            z80.initialize();
            
            loop();
        }
    }
    
    void loop() {
        try {
            for(;;) {
            	if (runZ80) {	//	TODO hacer que use la velocidad correcta y sea un thread distinto
            		int opcode = z80.readMemory(z80.PC);
    				z80.PC = (z80.PC + 1) & 0xFFFF;
            		z80.executeInstruction(opcode);
            	}
            	cpu.runInstruction();
            	bus.checkInterrupts();
            	vdp.run(24);
            	vdp.dmaFill();
            }
        } catch (RuntimeException e) {
            throw e;
        }
    }

	private int currentMultiplier = 1;
	public boolean runZ80 = false;
	
	void renderScreen() {
	    int m = currentMultiplier;
	    
	    for (int i = 0; i < 256; i++) {
	        for (int j = 0; j < 320; j++) {
	            int color = vdp.screenData[j][i];
	            
	            int pos = ((i * m) * (320 * m)) + (j * m);
	
	            pixels[pos] = color;
	        }
	    }
	    
	    jframe.repaint();
	}
	
//	PD5: Start or C
//	PD4: A or B
//	PD3: Right
//	PD2: Left
//	PD1: Down
//	PD0: Up
    private void keyPressedHandler(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
            	cpu.bus.joypad.U = 0;
            	break;
            case KeyEvent.VK_A:
            	cpu.bus.joypad.L = 0;
            	break;
            case KeyEvent.VK_D:
            	cpu.bus.joypad.R = 0;
                break;
            case KeyEvent.VK_S:
            	cpu.bus.joypad.D = 0;
                break;
            case KeyEvent.VK_E:
            	cpu.bus.joypad.S = 0;
                break;
            case KeyEvent.VK_T:
            	cpu.bus.joypad.A = 0;
                break;
            case KeyEvent.VK_Y:
            	cpu.bus.joypad.B = 0;
                break;
            case KeyEvent.VK_U:
            	cpu.bus.joypad.C = 0;
                break;
        }
    }

    private void keyReleasedHandler(KeyEvent e) {
        switch (e.getKeyCode()) {
	        case KeyEvent.VK_W:
	        	cpu.bus.joypad.U = 1;
	        	break;
	        case KeyEvent.VK_A:
	        	cpu.bus.joypad.L = 1;
	        	break;
	        case KeyEvent.VK_D:
	        	cpu.bus.joypad.R = 1;
	            break;
	        case KeyEvent.VK_S:
	        	cpu.bus.joypad.D = 1;
	            break;
	        case KeyEvent.VK_E:
	        	cpu.bus.joypad.S = 1;
	            break;
	        case KeyEvent.VK_T:
	        	cpu.bus.joypad.A = 1;
	            break;
	        case KeyEvent.VK_Y:
	        	cpu.bus.joypad.B = 1;
	            break;
	        case KeyEvent.VK_U:
	        	cpu.bus.joypad.C = 1;
	            break;
	    }
    }

}
