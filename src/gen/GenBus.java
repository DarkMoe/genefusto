package gen;

import java.util.Random;

//info z80 bus
//https://emu-docs.org/Genesis/gen-hw.txt
public class GenBus {

	Genefusto emu;
	GenMemory memory;
	GenVdp vdp;
	GenZ80 z80;
	GenJoypad joypad;
	Gen68 cpu;
	
	boolean writeSram;
	
	int[] sram = new int[0x200];
	
	//	https://emu-docs.org/Genesis/ssf2.txt
	boolean ssf2Mapper = false;
	
	int[] banks = new int[] {0, 1, 2, 3, 4, 5, 6, 7};
	
	GenBus(Genefusto emu, GenMemory memory, GenVdp vdp, GenZ80 z80, GenJoypad joypad, Gen68 cpu) {
		this.emu = emu;
		this.memory = memory;
		this.vdp = vdp;
		this.z80 = z80;
		this.joypad = joypad;
		this.cpu = cpu;
		
		initializeSram();
	}
	
	void initializeSram() {
//		sram[]	// TODO inicializar, e implementar bien la lectura/escritura (direccion / 2, y despues incrementa
		// de a 2 bytes desde la direccion inicial (porque aumenta de a 4, dividido 2 = 2)
	}
	
	public long read(long address, Size size) {
		address = address & 0xFF_FFFF;	// el memory map llega hasta ahi
		long data;
		
		if (ssf2Mapper && address >= 0x080000 && address <= 0x3FFFFF) {
			if (address >= 0x080000 && address <= 0x0FFFFF) {
				address = (banks[1] * 0x80000) + (address - 0x80000);
			} else if (address >= 0x100000 && address <= 0x17FFFF) {
				address = (banks[2] * 0x80000) + (address - 0x100000);
			} else if (address >= 0x180000 && address <= 0x1FFFFF) {
				address = (banks[3] * 0x80000) + (address - 0x180000);
			} else if (address >= 0x200000 && address <= 0x27FFFF) {
				address = (banks[4] * 0x80000) + (address - 0x200000);
			} else if (address >= 0x280000 && address <= 0x2FFFFF) {
				address = (banks[5] * 0x80000) + (address - 0x280000);
			} else if (address >= 0x300000 && address <= 0x37FFFF) {
				address = (banks[6] * 0x80000) + (address - 0x300000);
			} else if (address >= 0x380000 && address <= 0x3FFFFF) {
				address = (banks[7] * 0x80000) + (address - 0x380000);
			}
			
			if (size == Size.BYTE) {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					if (address < 0x200) {
						data = sram[(int) address];
					} else {
						data = 0;
					}
					
				} else {
					data = memory.readCartridgeByte(address);
				}
				
			} else if (size == Size.WORD) {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					data  = sram[(int) address] << 8;
					data |= sram[(int) address + 1];
				} else {
					data = memory.readCartridgeWord(address);
				}
				
			} else {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					data  = sram[(int) address] << 24;
					data |= sram[(int) address + 1] << 16;
					data |= sram[(int) address + 2] << 8;
					data |= sram[(int) address + 3];
					
				} else {
					data  = memory.readCartridgeWord(address) << 16;
					data |= memory.readCartridgeWord(address + 2);
					
				}
			}
			return data;
			
		}
		
		if (address <= 0x3F_FFFF) {
			if (size == Size.BYTE) {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					if (address < 0x200) {
						data = sram[(int) address];
					} else {
						data = 0;
					}
					
				} else {
					data = memory.readCartridgeByte(address);
				}
				
			} else if (size == Size.WORD) {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					data  = sram[(int) address] << 8;
					data |= sram[(int) address + 1];
				} else {
					data = memory.readCartridgeWord(address);
				}
				
			} else {
				if (address >= 0x200000 && address <= 0x20FFFF && writeSram) {
					address = address - 0x200000;
					data  = sram[(int) address] << 24;
					data |= sram[(int) address + 1] << 16;
					data |= sram[(int) address + 2] << 8;
					data |= sram[(int) address + 3];
					
				} else {
					data  = memory.readCartridgeWord(address) << 16;
					data |= memory.readCartridgeWord(address + 2);
					
				}
			}
			return data;
			
		} else if (address >= 0xA00000 && address <= 0xA0FFFF) {	//	Z80 addressing space
			return z80.readMemory((int) (address - 0xA00000));
			
		} else if (address == 0xA10000 || address == 0xA10001) {	//	Version register (read-only word-long)
			data = emu.getRegion();
			if (size == Size.BYTE) {
				return data;
			} else {
				return data << 8 | data;
			}
			
		} else if (address == 0xA10002 || address == 0xA10003) {	//	Controller 1 data
			return joypad.readDataRegister1();
			
		} else if (address == 0xA10004 || address == 0xA10005) {	//	Controller 2 data
			return joypad.readDataRegister2();
			
		} else if (address == 0xA10006 || address == 0xA10007) {	//	Expansion data
			return joypad.readDataRegister3();
		
		} else if (address == 0xA1000C || address == 0xA1000D) {	//	Expansion Port Control
			if (address == 0xA1000C) {
				return 0;
			} else if (address == 0xA1000D) {
				return 0;
			}
			
		} else if (address == 0xA10008 || address == 0xA10009) {	//	Controller 1 control
			if (size == Size.BYTE) {
				return joypad.readControlRegister1() & 0xFF;
			} else {
				return joypad.readControlRegister1();
			}
			
		} else if (address == 0xA1000A || address == 0xA1000B) {	//	Controller 2 control
			if (size == Size.BYTE) {
				return joypad.readControlRegister2() & 0xFF;
			} else {
				return joypad.readControlRegister2();
			}
			
		} else if (address == 0xA11100 || address == 0xA11101) {	//	Z80 bus request	
//			return (z80.busRequested && !z80.reset) ? 0 : 1;
			return new Random().nextBoolean() ? 1: 0;
//			return 0;	//	FIXME hacer esto bien
		
		} else if (address == 0xC00000 || address == 0xC00002) {	// VDP Data
			if (size == Size.BYTE) {
				return (vdp.readDataPort(size) >> 8);
			} else if (size == Size.WORD) {
				return (vdp.readDataPort(size));
			} else {
				data  = vdp.readDataPort(size) << 16;
				data |= vdp.readDataPort(size);
			}

		} else if (address == 0xC00001 || address == 0xC00003) {	// VDP Data
			return (vdp.readDataPort(size) & 0xFF);
			
		} else if (address == 0xC00004 || address == 0xC00006) {	// VDP Control
			data = vdp.readControl(); 
			if (size == Size.WORD) {
				return data;
			} else if (size == Size.BYTE) {
				return data >> 8;
			} else {
				throw new RuntimeException();
			}

		} else if (address == 0xC00005 || address == 0xC00007) {
			data = vdp.readControl(); 
			if (size == Size.BYTE) {
				return data & 0xFF;
			} else {
				throw new RuntimeException("");
			}
			
		} else if (address == 0xC00008 || address == 0xC00009) {
			int v = vdp.line;
			int h = new Random().nextInt(256);
			if (size == Size.WORD) {
				return (v << 8) | h;	//	VDP HV counter
			} else if (size == Size.BYTE) {
				if (address == 0xC00008) {
					return v;
				} else {
					return h;
				}
			}
			
		} else if (address >= 0xFF0000) {
			if (size == Size.BYTE) {
				return memory.readRam(address);
			} else if (size == Size.WORD) {
				data  = memory.readRam(address) << 8;
				data |= memory.readRam(address + 1);
				return data;
			} else {
				data  = memory.readRam(address) << 24;
				data |= memory.readRam(address + 1) << 16;
				data |= memory.readRam(address + 2) << 8;
				data |= memory.readRam(address + 3);
				return data;
			}
			
		} else {
			System.out.println("NOT MAPPED: " + pad4(address) + " - " + pad4(cpu.PC));
		}
		
		return 0;
	}
	
//	https://wiki.megadrive.org/index.php?title=IO_Registers
	public void write(long address, long data, Size size) {
		long addressL = (address & 0xFF_FFFF);
		if (size == Size.BYTE) {
			data = data & 0xFF;
		} else if (size == Size.WORD) {
			data = data & 0xFFFF;
		} else {
			data = data & 0xFFFF_FFFFL;
		}
		
		if (addressL <= 0x3FFFFF) {	//	Cartridge ROM/RAM
			if (addressL >= 0x200000 && address <= 0x20FFFF && writeSram) {
				addressL = addressL - 0x200000;
				
				if (size == Size.BYTE) {
					if (address < 0x200) {
						sram[(int) addressL] = (int) data;
					}
					
				} else if (size == Size.WORD) {
					sram[(int) addressL] = (int) (data >> 8) & 0xFF;
					sram[(int) addressL + 1] = (int) data & 0xFF;
				} else {
					sram[(int) addressL] = (int) (data >> 24) & 0xFF;
					sram[(int) addressL + 1] = (int) (data >> 16) & 0xFF;
					sram[(int) addressL + 2] = (int) (data >> 8) & 0xFF;
					sram[(int) addressL + 3] = (int) data & 0xFF;
				}
				
			} else {
				System.out.println("write cart rom ram ? " + Integer.toHexString((int) addressL));
			}
			
		} else if (addressL >= 0xA00000 && addressL <= 0xA0FFFF) {	//	Z80 addressing space
			int addr = (int) (address - 0xA00000);
			if (size == Size.BYTE) {
				z80.writeByte(addr, data);
			} else if (size == Size.WORD) {
				z80.writeWord(addr, data);
			} else {
				z80.writeWord(addr, data >> 16);
				z80.writeWord(addr + 2, data & 0xFFFF);
			}
			
//			System.out.println("Z80: " + pad4(addr) + " " + pad((int) data));
			
		} else if (address == 0xA10002 || address == 0xA10003) {	//	Controller 1 data
			joypad.writeDataRegister1(data);
			
		} else if (address == 0xA10004 || address == 0xA10005) {	//	Controller 2 data
			joypad.writeDataRegister2(data);
		
		} else if (address == 0xA10006 || address == 0xA10007) {	//	Expansion port data
			// ???
			
		} else if (addressL == 0xA10009) {	//	Controller 1 control
			joypad.writeControlRegister1(data);
			
		} else if (addressL == 0xA1000B) {	//	Controller 2 control
			joypad.writeControlRegister2(data);
			
		} else if (addressL == 0xA1000D) {	//	Controller 2 control
			joypad.writeControlRegister3(data);
		
		} else if (address == 0xA10012 || address == 0xA10013) {	//	Controller 1 serial control
			System.out.println("IMPL CONTR 1 !!");
			
		} else if (address == 0xA10018 || address == 0xA10019) {	//	Controller 2 serial control
			System.out.println("IMPL CONTR 2 !!");
		
		} else if (address == 0xA1001E || address == 0xA1001F) {	//	Expansion port serial control
			System.out.println("expansion port serial control !!");
			
		} else if (addressL == 0xA11100 || addressL == 0xA11101) {	//	Z80 bus request
			//	To stop the Z80 and send a bus request, #$0100 must be written to $A11100.
			if (data == 0x0100 || data == 0x1) {
				z80.requestBus();
				emu.runZ80 = false;
				
			//	 #$0000 needs to be written to $A11100 to return the bus back to the Z80
			} else if (data == 0x0000) {
				z80.unrequestBus();
				if (!z80.reset) {
					emu.runZ80 = true;
				}
				
			}
		} else if (addressL == 0xA11200 || addressL == 0xA11201) {	//	Z80 bus reset
			//	if the Z80 is required to be reset (for example, to load a new program to it's memory)
			//	this may be done by writing #$0000 to $A11200, but only when the Z80 bus is requested
			if (data == 0x0000) {
//				if (z80.busRequested) {
					z80.reset();
//				} else {
					z80.initialize();
					emu.runZ80 = false;
//				}
				
			//	After returning the bus after loading the new program to it's memory,
			//	the Z80 may be let go from reset by writing #$0100 to $A11200.
			} else if (data == 0x0100 || data == 0x1) {
				if (z80.busRequested) {
					z80.disableReset();

				} else {
					z80.disableReset();
//					z80.initialize();
					emu.runZ80 = true;
				}
			}
			
		} else if (addressL == 0xA130F1) {	//	Sonic 3 will write to this register to enable and disable writing to its save game memory
			System.out.println("SRAM Register enable: " + Integer.toHexString((int) data));
			if (data == 0) {
				writeSram = false;
			} else {
				writeSram = true;
			}
			
		} else if (addressL == 0xA130F3 && ssf2Mapper) {	//	0x080000 - 0x0FFFFF
			data = data & 0x3F;	//	A page is specified with 6 bits (bits 7 and 6 are always 0) thus allowing a possible 64 pages (SSFII only has 10, though.)
			banks[1] = (int) data;
			
		} else if (addressL == 0xA130F5 && ssf2Mapper) {	//	0x100000 - 0x17FFFF
			data = data & 0x3F;
			banks[2] = (int) data;
			
		} else if (addressL == 0xA130F7 && ssf2Mapper) {	//	0x180000 - 0x1FFFFF
			data = data & 0x3F;
			banks[3] = (int) data;
			
		} else if (addressL == 0xA130F9 && ssf2Mapper) {	//	0x200000 - 0x27FFFF
			data = data & 0x3F;
			banks[4] = (int) data;
			
		} else if (addressL == 0xA130FB && ssf2Mapper) {	//	0x280000 - 0x2FFFFF
			data = data & 0x3F;
			banks[5] = (int) data;
			
		} else if (addressL == 0xA130FD && ssf2Mapper) {	//	0x300000 - 0x37FFFF
			data = data & 0x3F;
			banks[6] = (int) data;
			
		} else if (addressL == 0xA130FF && ssf2Mapper) {	//	0x380000 - 0x3FFFFF
			data = data & 0x3F;
			banks[7] = (int) data;
			
		} else if (address == 0xA14000) {	//	VDP TMSS
			System.out.println("TMSS: " + Integer.toHexString((int) data));
			
		} else if (addressL == 0xC00000 || addressL == 0xC00001
				|| addressL == 0xC00002 || addressL == 0xC00003) {	// word / long word
			vdp.writeDataPort((int) data, size);
			
		} else if (addressL == 0xC00004 || addressL == 0xC00005
				|| addressL == 0xC00006 || addressL == 0xC00007) {	// word / long word
			if (size == Size.BYTE) {
				throw new RuntimeException();
			} else if (size == Size.WORD) {
				vdp.writeControlPort(data);
			} else {
				vdp.writeControlPort(data >> 16);
				vdp.writeControlPort(data & 0xFFFF);
			}

		} else if (addressL == 0xC00011) {	//	PSG output
//			System.out.println("PSG Output");
			// TODO implement audio		http://md.squee.co/PSG
			
		} else if (addressL >= 0xFF0000) {
			long addr = (addressL & 0xFFFFFF) - 0xFF0000;
			
			if (size == Size.BYTE) {
				memory.writeRam(addr, data);
			} else if (size == Size.WORD) {
				memory.writeRam(addr, (data >> 8));
				memory.writeRam(addr + 1, (data & 0xFF));
			} else if (size == Size.LONG) {
				memory.writeRam(addr, (data >> 24) & 0xFF);
				memory.writeRam(addr + 1, (data >> 16) & 0xFF);
				memory.writeRam(addr + 2, (data >> 8) & 0xFF);
				memory.writeRam(addr + 3, (data & 0xFF));
			}
			
		} else {
			System.out.println("WRITE NOT SUPPORTED ! " + Integer.toHexString((int) address) + " - PC: " + Integer.toHexString((int) cpu.PC));
//			throw new RuntimeException("WRITE NOT SUPPORTED ! " + Integer.toHexString((int) address) + " - PC: " + Integer.toHexString((int) cpu.PC));
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

	boolean hintFrameTaken = false;
	
	int hLinesPassed = 0;
	private boolean vintPending;
	boolean hintPending;
	
	//	https://www.gamefaqs.com/genesis/916377-genesis/faqs/9755
	//	http://darkdust.net/writings/megadrive/initializing
	public void checkInterrupts() {
		if (vdp.ie0) {				//	vint on
			if (vdp.vip == 1) {		//	level 6 interrupt
				vintPending = true;
			}
		}
		
		if (vdp.ie1) {
//			int intLine = hLinesPassed;
//			if (intLine != 0 && vdp.line == hLinesPassed) {
//				hintPending = true;
//				hLinesPassed = vdp.registers[0xA];
//			}
		}
		
		int mask = cpu.getInterruptMask();
		
		if (vintPending && mask < 0x6) {
			cpu.stop = false;
			
			long oldPC = cpu.PC;
			int oldSR = cpu.SR;
			long ssp = cpu.SSP;
			
			ssp--;
			write(ssp, oldPC & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 8) & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 16) & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 24), Size.BYTE);
			
			ssp--;
			write(ssp, oldSR & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldSR >> 8) & 0xFF, Size.BYTE);
			
			long address = readInterruptVector(0x78);
			cpu.PC = address;
			cpu.SR = (cpu.SR & 0xF8FF) | 0x0600;

			cpu.SR |= 0x2000;	// force supervisor mode
			
			cpu.setALong(7, ssp);
			
			vdp.vip = 0;
			
			vintPending = false;
			
			return;
		}
		
		if (hintPending && vdp.ie1 && mask < 0x4) {
			cpu.stop = false;
			
			long oldPC = cpu.PC;
			int oldSR = cpu.SR;
			long ssp = cpu.SSP;
			
			System.out.println("HINT ! Line: " + Integer.toHexString(vdp.line));
			
			ssp--;
			write(ssp, oldPC & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 8) & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 16) & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldPC >> 24), Size.BYTE);
			
			ssp--;
			write(ssp, oldSR & 0xFF, Size.BYTE);
			ssp--;
			write(ssp, (oldSR >> 8) & 0xFF, Size.BYTE);
			
			long address = readInterruptVector(0x70);
			cpu.PC = address;
			cpu.SR = (cpu.SR & 0xF8FF) | 0x0400;

			cpu.SR |= 0x2000;	// force supervisor mode
			
			cpu.setALong(7, ssp);
			
			hintPending = false;
		}
	}

	public long readInterruptVector(long vector) {
		long address  = memory.readCartridgeWord(vector) << 16;
			 address |= memory.readCartridgeWord(vector + 2);
		return address;
	}
	
}
