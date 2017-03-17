package gen;

public class GenBus {

	Genefusto emu;
	GenMemory memory;
	GenVdp vdp;
	GenZ80 z80;
	GenJoypad joypad;
	Gen68 cpu;
	
	GenBus(Genefusto emu, GenMemory memory, GenVdp vdp, GenZ80 z80, GenJoypad joypad, Gen68 cpu) {
		this.emu = emu;
		this.memory = memory;
		this.vdp = vdp;
		this.z80 = z80;
		this.joypad = joypad;
		this.cpu = cpu;
	}
	
	public long read(long address) {
		address = address & 0xFFFFFF;	// el memory map llega hasta ahi
		long data;
		if (address <= 0x3FFFFF) {
			data = memory.readCartridge(address);
			return data;
			
		} else if (address == 0xA10000 || address == 0xA10001) {	//	Version register (read-only word-long)
			return 0xA0;	//	US:	A1A1 o A0A0 ?	EU:	C1C1	US SEGA CD:	8181	(las 2 direcciones devuelven lo mismo)
			
		} else if (address == 0xA10002 || address == 0xA10003) {	//	Controller 1 data
			return joypad.readDataRegister1();
			
		} else if (address == 0xA10004 || address == 0xA10005) {	//	Controller 2 data
			return joypad.readDataRegister1();
		
		} else if (address == 0xA1000C || address == 0xA1000D) {	//	Expansion Port Control
			if (address == 0xA1000C) {
				return 0;
			} else if (address == 0xA1000D) {
				return 0;
			}
			
		} else if (address == 0xA10008 || address == 0xA10009) {	//	Controller 1 control
			if (address == 0xA10008) {
				return 0;
			} else if (address == 0xA10009) {
				return 0;
			}
			
		} else if (address == 0xA1000A || address == 0xA1000B) {	//	Controller 2 control
			if (address == 0xA1000A) {
				return 0;
			} else if (address == 0xA1000B) {
				return 0;
			}
			
		} else if (address == 0xA11100 || address == 0xA11101) {	//	Z80 bus request	
			return (z80.reset) ? 1 : 0;
		
		} else if (address == 0xC00004 || address == 0xC00006) {	// VDP Control
			return (vdp.readControl() >> 8);

		} else if (address == 0xC00005 || address == 0xC00007) {
			return (vdp.readControl() & 0xFF);
			
		} else if (address >= 0xFF0000) {
			return memory.readRam(address);
			
		} else {
			throw new RuntimeException("NOT MAPPED: " + pad4(address));
		}
		
		return 0;
	}
	
//	https://wiki.megadrive.org/index.php?title=IO_Registers
	public void write(long address, long data, Size size) {
		long addressL = (address & 0xFFFFFFFFL);
		if (size == Size.byt) {
			data = data & 0xFF;
		} else if (size == Size.word) {
			data = data & 0xFFFF;
		} else {
			data = data & 0xFFFF;	// manejado afuera, quias deberia manejarse tambien aca
		}
		
		if (addressL >= 0xA00000 && addressL <= 0xA0FFFF) {	//	Z80 addressing space
			int addr = (int) (address - 0xA00000);
			if (size == Size.byt) {
				z80.memory[addr] = (int) data;
			} else {
				z80.memory[addr] = (int) (data >> 8);
				z80.memory[addr + 1] = (int) (data & 0xFF);
			}
			
			System.out.println("Z80: " + pad4(addr) + " " + pad((int) data));
			
		} else if (address == 0xA10002 || address == 0xA10003) {	//	Controller 1 data
			joypad.writeDataRegister1(data);
			
		} else if (address == 0xA10004 || address == 0xA10005) {	//	Controller 2 data
			joypad.writeDataRegister2(data);
			
		} else if (addressL == 0xA10009) {	//	Controller 1 control
			joypad.writeControlRegister1(data);
			
		} else if (addressL == 0xA1000B) {	//	Controller 2 control
			joypad.writeControlRegister2(data);
			
		} else if (addressL == 0xA1000D) {	//	Controller 2 control
			joypad.writeControlRegister3(data);
			
		} else if (addressL == 0xA11100 || addressL == 0xA11101) {	//	Z80 bus request
			//	To stop the Z80 and send a bus request, #$0100 must be written to $A11100.
			if (data == 0x0100) {
				z80.requestBus();
				
			//	 #$0000 needs to be written to $A11100 to return the bus back to the Z80
			} else if (data == 0x0000) {
				z80.devolverBus();
				z80.reset = false;
				emu.runZ80 = false;
			}
		} else if (addressL == 0xA11200 || addressL == 0xA11201) {	//	Z80 bus reset
			//	if the Z80 is required to be reset (for example, to load a new program to it's memory)
			//	this may be done by writing #$0000 to $A11200, but only when the Z80 bus is requested
			if (data == 0x0000 && z80.isBusRequested()) {
				z80.reset();
				
			//	After returning the bus after loading the new program to it's memory,
			//	the Z80 may be let go from reset by writing #$0100 to $A11200.
			} else if (data == 0x0100) {
				z80.reset = false;
				if (!z80.busRequested) {
					emu.runZ80 = true;
				}
			} else {
				System.out.println("Z80 OJO ! .. algo del bus del z80 no se hizo aca");
			}
			
		} else if (addressL == 0xC00000 || addressL == 0xC00001
				|| addressL == 0xC00002 || addressL == 0xC00003) {	// word / long word
			vdp.writeDataPort((int) data);
			
		} else if (addressL == 0xC00004 || addressL == 0xC00005
				|| addressL == 0xC00006 || addressL == 0xC00007) {	// word / long word
			vdp.writeControlPort(data);

		} else if (addressL == 0xC00011) {	//	PSG output
			System.out.println("PSG Output");
			// TODO implement audio		http://md.squee.co/PSG
			
		} else if (addressL >= 0xFF0000) {
			long addr = (addressL & 0xFFFFFF) - 0xFF0000;
			
			if (size == Size.byt) {
				memory.writeRam(addr, data);
			} else {
				memory.writeRam(addr, (data >> 8));
				memory.writeRam(addr + 1, (data & 0xFF));
			}
			
		} else {
			throw new RuntimeException("WRITE NOT SUPPORTED ! " + Integer.toHexString((int) address));
		}
	}
	
	public final String pad4(long reg) {
        String s = Long.toHexString(reg).toUpperCase();
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }
	
	public final String pad(int reg) {
        String s = Integer.toHexString(reg).toUpperCase();
        while (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }

	public void outPort(int tmp, int a) {
		throw new RuntimeException("Z80 !");
	}

	public int inPort(int tmp) {
		throw new RuntimeException("Z80 !");
	}

	//	https://www.gamefaqs.com/genesis/916377-genesis/faqs/9755
	//	http://darkdust.net/writings/megadrive/initializing
	public void checkInterrupts() {
		if (vdp.vip == 1) {		//	level 6 interrupt
			int mask = cpu.getInterruptMask();
			if (mask <= 0x6) {
				long oldPC = cpu.PC;
				int oldSR = cpu.SR;
				int ssp = cpu.SSP;
				
				ssp--;
				write(ssp, oldPC & 0xFF, Size.byt);
				ssp--;
				write(ssp, (oldPC >> 8) & 0xFF, Size.byt);
				ssp--;
				write(ssp, (oldPC >> 16) & 0xFF, Size.byt);
				ssp--;
				write(ssp, (oldPC >> 24), Size.byt);
				
				ssp--;
				write(ssp, oldSR & 0xFF, Size.byt);
				ssp--;
				write(ssp, (oldSR >> 8) & 0xFF, Size.byt);
				
				cpu.SSP = ssp;
				cpu.A[7] = ssp;
				
				long address = readInterruptVector(0x78);
				cpu.PC = address;
				cpu.SR = (cpu.SR & 0xF8FF) | 0x0600;
				
				vdp.vip = 0;
			}
		}
	}

	private long readInterruptVector(long vector) {
		long address  = memory.readCartridge(vector) << 24;
			 address |= memory.readCartridge(vector + 1) << 16;
			 address |= memory.readCartridge(vector + 2) << 8;
			 address |= memory.readCartridge(vector + 3) << 0;
		return address;
	}
	
}
