package gen;

//Start	End	Description
//0000h	1FFFh	Z80 RAM
//2000h	3FFFh	Reserved
//4000h	YM2612 A0
//4001h	YM2612 D0
//4002h	YM2612 A1
//4003h	YM2612 D1
//4004h	5FFFh	Reserved
//6000h	Bank register
//6001h	7F10h	Reserved
//7F11h	SN76489 PSG
//7F12h	7FFFh	Reserved
//8000h	FFFFh	M68k memory bank

//The current bank to be at the 8000h and up region can be controlled by the banking register.

//If the 68k wishes to access anything in the Z80 address space, the Z80 must be stopped.
//This can be accomplished through the register at $A11100. To stop the Z80 and send a bus request, #$0100 must be written to $A11100.
//To see if the Z80 has stopped, bit 0 of $A11100 must be checked - if it's clear, the 68k may access the bus, but wait if it is set.
//Once access to the Z80 area is complete, #$0000 needs to be written to $A11100 to return the bus back to the Z80.
//No waiting to see if the bus is returned is required here — it is handled automatically.
//However, if the Z80 is required to be reset (for example, to load a new program to it's memory) this may be done by writing #$0000 to $A11200, but only when the Z80 bus is requested.
//After returning the bus after loading the new program to it's memory, the Z80 may be let go from reset by writing #$0100 to $A11200.

//	http://md.squee.co/Zilog_Z80

//		 Bit 7 6 5 4 3 2   1 0
//	Position S Z X H X P/V N C
public class GenZ80 {

	int memory[] = new int[0x2000];
	
	int A;
	int B;
	int C;
	int D;
	int E;
	int F;
	int H;
	int L;
	
//	Se usa en Interrupt Mode 2
	//	http://www.smspower.org/Development/InterruptMechanism
	int I = 0;	//	I is the interrupt vector register. It is used by the calculator in the interrupt 2 mode.
	
	int A2;
	int B2;
	int C2;
	int D2;
	int E2;
	int F2;
	int H2;
	int L2;
	
	int IX, IY;
	int R;

	GenBus bus;
	
	int PC;
	int SP;
	
    boolean enableInterrupts;
    boolean enableInterruptsNextInstr;
	int interruptMode;
	
	int[] flagsSZ = new int[256];
	
	public GenZ80(GenBus bus) {
		this.bus = bus;
		
		initCache();
	}
	
	void initCache() {
		for (int i = 0; i < 256; i++) {
			if ((i & 0x80) > 0) {
				flagsSZ[i] |= 0x80;
			}
			flagsSZ[0] |= 0x40;
		}
	}
	
	StringBuilder lineLog = new StringBuilder();
	boolean toPrint = false;
	boolean disableInterruptsNow;
	boolean halted;
	
	String[] lastInstr = new String[10];
	private boolean FF1;
	private boolean FF2;
	boolean busRequested;
	boolean reset;
	
	int executeInstruction(int opcode) {
		int tmp, addr, lo, hi;
		int cycles = 0;
		
		String fullOpcode = hex(opcode);
        if (toPrint) {
//            lineLog.setLength(0);
//            String video = "";
//            int[] vRegs = this.video.vRegs;
//            for (int i = 0; i < vRegs.length; i++) {
//				video += " - V" + i + ": " + hex(vRegs[i]);
//			}
//            lineLog.append("\naf: ").append(hex(A)).append(hex(F)).append(" - bc: ").append(hex(B))
//                .append(hex(C)).append(" - de: ").append(hex(D)).append(hex(E)).append(" - hl: ")
//                .append(hex(H)).append(hex(L)).append(" - IX:").append(hex4(IX)).append(" - IY:").append(hex4(IY)).append(" - R: ").append(hex(R)).append(" - bank1: " + hex(memory.bank1)).append(" - bank2: " + hex(memory.bank2)).append(" - bank3: " + hex(memory.bank3))
//                .append("\npc: ").append(hex4(PC - 1)).append(" - sp: ").append(hex4(SP)).append(" - opcode: ").append(fullOpcode).append(" - VAddress: " + hex4(io.getControlWord()))
//                .append(" - vCounter: ").append(io.getVCounter()).append(" - vdpStatus: ").append(hex(io.getStatus())).append("\n")
//                .append(video);
//                
//            System.out.println(lineLog.toString());
        }
        
//		for (int i = 0; i < lastInstr.length - 1; i++) {
//			lastInstr[i] = lastInstr[i + 1];
//		} 
//		lastInstr[9] = hex4(PC) + ": " + hex(opcode);
//		if (opcode == 0xED || opcode == 0xDD || opcode == 0xCB || opcode == 0xFD) {
//			lastInstr[9] = lastInstr[9] + " " + hex(readMemory(PC));
//		}

        switch (opcode) {
        case 0x00:	// NOP
        	cycles = 4;
            break;
        case 0x01:
        	lo = readMemory(PC);
            hi = readMemory(PC + 1);

            tmp = (hi << 8) | lo;
            BC(tmp);
            
            PC = (PC + 2) & 0xFFFF;
            cycles = 10;
            break;
        case 0x02:
            writeMemory(BC(), A);
            cycles = 7;
            break;
        case 0x03:	//	INC BC
            BC((BC() + 1) & 0xFFFF);
            cycles = 6;
            break;
        case 0x04:
            B = cpuInc(B);
            cycles = 4;
            break;
        case 0x05:
            B = cpuDec(B);
            cycles = 4;
            break;
        case 0x06:  // ld b,*
            B = readMemory(PC);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0x07:	// RLCA
            rlca();
            cycles = 4;
            break;
        case 0x08:  //  EX af, af'
        	tmp = AF();
        	AF(AF2());
        	AF2(tmp);
        	
        	cycles = 4;
            break;
        case 0x09:	//	ADD HL,BC
            tmp = add16Bits(HL(), BC());
            HL(tmp);
            
            cycles = 11;
            break;
        case 0x0A:  // ld a,(bc)
            A = readMemory(BC());
            
            cycles = 7;
            break;
        case 0x0B:	// DEC BC
            BC((BC() - 1) & 0xFFFF);
            cycles = 6;
            break;
        case 0x0C:
            C = cpuInc(C);
            cycles = 4;
            break;
        case 0x0D:
            C = cpuDec(C);
            cycles = 4;
            break;
        case 0x0E:  // ld c,*
            C = readMemory(PC);
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0x0F:	//	RRCA
            rrca();
            cycles = 4;
            break;
		// Decreases B and jumps to a label if not zero. Note that DJNZ does a
		// relative jump, so it can only jump between 128 bytes back/ahead.
        //	13 cycles / 8 cycles
        case 0x10:
        	B = (B - 1) & 0xFF;
        	if (B != 0) {
        		byte n = (byte) readMemory(PC);
                PC += n;
                cycles = 13;
        	} else {
        		cycles = 8;
        	}
        	
            PC = (PC + 1) & 0xFFFF;
            break;
        case 0x11:  // ld de,**
        	lo = readMemory(PC);
        	hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;

            DE(tmp);
            
            PC = (PC + 2) & 0xFFFF;
            cycles = 10;
            break;
        case 0x12:	//	ld (de),a
            writeMemory(DE(), A);
            cycles = 7;
            break;
        case 0x13:	//	INC DE
            DE((DE() + 1) & 0xFFFF);
            cycles = 6;
            break;
        case 0x14:
            D = cpuInc(D);
            cycles = 4;
            break;
        case 0x15:
            D = cpuDec(D);
            cycles = 4;
            break;
        case 0x16:  // ld d,*
            D = readMemory(PC);

            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0x17:	//	RLA
            rla();
            cycles = 4;
            break;
        case 0x18:  // jr *
            byte n = (byte) readMemory(PC);
            PC += n;

            PC = (PC + 1) & 0xFFFF;
            cycles = 12;
            break;
        case 0x19:	//	ADD HL,DE
            tmp = add16Bits(HL(), DE());
            HL(tmp);
            
            cycles = 11;
            break;
        case 0x1A:  // ld a,(de)
            A = readMemory(DE());
            
            cycles = 7;
            break;
        case 0x1B:	//	DEC DE
            DE((DE() - 1) & 0xFFFF);
            
            cycles = 6;
            break;
        case 0x1C:
            E = cpuInc(E);
            
            cycles = 4;
            break;
        case 0x1D:
            E = cpuDec(E);
            
            cycles = 4;
            break;
        case 0x1E:  // ld e,*
            E = readMemory(PC);

            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0x1F:	//	RRA
            rra();
            cycles = 4;
            break;
        case 0x20:  // jr nz,*
            cycles = jump(!isZeroFlagSet());
            break;
        case 0x21:  // ld hl,**
        	lo = readMemory(PC);
        	hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
            
            HL(tmp);
            
            PC = (PC + 2) & 0xFFFF;
            cycles = 10;
            break;
        case 0x22:  // ld (**),hl
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            addr = (hi << 8) | lo;
            
            writeMemory(addr, L);
            writeMemory(addr + 1, H);
            
            PC = (PC + 2) & 0xFFFF;
            cycles = 16;
            
            break;
        case 0x23:	//	INC HL
            HL((HL() + 1) & 0xFFFF);
            
            cycles = 6;
            break;
        case 0x24:
            H = cpuInc(H);
            cycles = 4;
            break;
        case 0x25:
            H = cpuDec(H);
            cycles = 4;
            break;
        case 0x26:  // ld h,*
            H = readMemory(PC);

            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0x27:
            cpuDaa();
            cycles = 4;
            break;
        case 0x28:  // jr z,*
            cycles = jump(isZeroFlagSet());
            break;
        case 0x29:	//	ADD HL,HL
            tmp = add16Bits(HL(), HL());
            HL(tmp);
            
            cycles = 11;
            break;
//	The contents of memory address (nn) are loaded to the low-order portion of register pair
//	HL (Register L), and the contents of the next highest memory address (nn + 1) are loaded
//	to the high-order portion of HL (Register H). The first n operand after the op code is the
//	low-order byte of nn
        case 0x2A:  // ld hl,(**)
        	lo = readMemory(PC);
        	hi = readMemory(PC + 1);
        	tmp = (hi << 8) | lo;
        	
            L = readMemory(tmp);
            H = readMemory(tmp + 1);
            
            PC = (PC + 2) & 0xFFFF;
            cycles = 16;
            
            break;
        case 0x2B:	//	DEC HL
            HL((HL() - 1) & 0xFFFF);
            cycles = 6;
            break;
        case 0x2C:
            L = cpuInc(L);
            cycles = 4;
            break;
        case 0x2D:
            L = cpuDec(L);
            cycles = 4;
            break;
        case 0x2E:  // ld l,*
            L = readMemory(PC);

            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0x2F:
            cpl();
            cycles = 4;
            break;
        case 0x30:  // jr nc,*
            cycles = jump(!isCarryFlagSet());
            break;
        case 0x31:  // ld sp,**
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
     
            SP = tmp;

            PC = (PC + 2) & 0xFFFF;
            cycles = 10;
            break;
        case 0x32:  // ld   (nn),a
        	lo = readMemory(PC);
            hi = readMemory(PC + 1);
            addr = (hi << 8) | lo;
            
            writeMemory(addr, A);
            
            PC = (PC + 2) & 0xFFFF;
            cycles = 13;
            
            break;
        case 0x33:	//	INC SP
            SP = (SP + 1) & 0xFFFF;
            cycles = 6;
            break;
        case 0x34:	//	inc (hl)
            tmp = readMemory(HL());
            tmp = cpuInc(tmp);
            writeMemory(HL(), tmp);
            
            cycles = 11;
            break;
        case 0x35:	//	dec (hl)
            tmp = readMemory(HL());
            tmp = cpuDec(tmp);
            writeMemory(HL(), tmp);
            
            cycles = 11;
            break;
        case 0x36:	//	ld (hl),*
            tmp = readMemory(PC);
            writeMemory(HL(), tmp);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 10;
            break;
        case 0x37:
            sfc();
            cycles = 4;
            break;
        case 0x38:	//	jr c,*
            cycles = jump(isCarryFlagSet());
            break;
        case 0x39:	//	ADD HL,SP
            tmp = add16Bits(HL(), SP);
            HL(tmp);
            
            cycles = 11;
            break;
        case 0x3A:	//	ld a,(**)
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
       
            A = readMemory(tmp);
            
            PC = (PC + 2) & 0xFFFF;
            cycles = 13;
            
            break;
        case 0x3B:	//	DEC SP
            SP = (SP - 1) & 0xFFFF;
            cycles = 6;
            break;
        case 0x3C:
            A = cpuInc(A);
            cycles = 4;
            break;
        case 0x3D:
            A = cpuDec(A);
            cycles = 4;
            break;
        case 0x3E:	//	ld a,*
            A = readMemory(PC);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0x3F:
            ccf();
            cycles = 4;
            break;
            
        case 0x40:
            // B = B;
        	cycles = 4;
            break;
        case 0x41:
            B = C;
            cycles = 4;
            break;
        case 0x42:
            B = D;
            cycles = 4;
            break;
        case 0x43:
            B = E;
            cycles = 4;
            break;
        case 0x44:
            B = H;
            cycles = 4;
            break;
        case 0x45:
            B = L;
            cycles = 4;
            break;
        case 0x46:
            B = readMemory(HL());
            cycles = 7;
            break;
        case 0x47:
            B = A;
            cycles = 4;
            break;
            
        case 0x48:
            C = B;
            cycles = 4;
            break;
        case 0x49:
            // C = C;
        	cycles = 4;
            break;
        case 0x4A:
            C = D;
            cycles = 4;
            break;
        case 0x4B:
            C = E;
            cycles = 4;
            break;
        case 0x4C:
            C = H;
            cycles = 4;
            break;
        case 0x4D:
            C = L;
            cycles = 4;
            break;
        case 0x4E:
            C = readMemory(HL());
            cycles = 7;
            break;
        case 0x4F:
            C = A;
            cycles = 4;
            break;
            
        case 0x50:
            D = B;
            cycles = 4;
            break;
        case 0x51:
            D = C;
            cycles = 4;
            break;
        case 0x52:
            // D = D;
        	cycles = 4;
            break;
        case 0x53:
            D = E;
            cycles = 4;
            break;
        case 0x54:
            D = H;
            cycles = 4;
            break;
        case 0x55:
            D = L;
            cycles = 4;
            break;
        case 0x56:
            D = readMemory(HL());
            cycles = 7;
            break;
        case 0x57:
            D = A;
            cycles = 4;
            break;
            
        case 0x58:
            E = B;
            cycles = 4;
            break;
        case 0x59:
            E = C;
            cycles = 4;
            break;
        case 0x5A:
            E = D;
            cycles = 4;
            break;
        case 0x5B:
            // E = E;
        	cycles = 4;
            break;
        case 0x5C:
            E = H;
            cycles = 4;
            break;
        case 0x5D:
            E = L;
            cycles = 4;
            break;
        case 0x5E:
            E = readMemory(HL());
            cycles = 7;
            break;
        case 0x5F:
            E = A;
            cycles = 4;
            break;
            
        case 0x60:
            H = B;
            cycles = 4;
            break;
        case 0x61:
            H = C;
            cycles = 4;
            break;
        case 0x62:
            H = D;
            cycles = 4;
            break;
        case 0x63:
            H = E;
            cycles = 4;
            break;
        case 0x64:
            // H = H;
        	cycles = 4;
            break;
        case 0x65:
            H = L;
            cycles = 4;
            break;
        case 0x66:
            H = readMemory(HL());
            cycles = 7;
            break;            
        case 0x67:
            H = A;
            cycles = 4;
            break;
            
        case 0x68:
            L = B;
            cycles = 4;
            break;
        case 0x69:
            L = C;
            cycles = 4;
            break;
        case 0x6A:
            L = D;
            cycles = 4;
            break;
        case 0x6B:
            L = E;
            cycles = 4;
            break;
        case 0x6C:
            L = H;
            cycles = 4;
            break;
        case 0x6D:
            // L = L;
        	cycles = 4;
            break;
        case 0x6E:
            L = readMemory(HL());
            cycles = 7;
            break;
        case 0x6F:
            L = A;
            cycles = 4;
            break;
            
        case 0x70:
            writeMemory(HL(), B);
            cycles = 7;
            break;
        case 0x71:
            writeMemory(HL(), C);
            cycles = 7;
            break;
        case 0x72:
            writeMemory(HL(), D);
            cycles = 7;
            break;
        case 0x73:
            writeMemory(HL(), E);
            cycles = 7;
            break;
        case 0x74:
            writeMemory(HL(), H);
            cycles = 7;
            break;
        case 0x75:
            writeMemory(HL(), L);
            cycles = 7;
            break;
        case 0x76:
            halted = true;
            cycles = 4;
            break;
        case 0x77:
            writeMemory(HL(), A);
            cycles = 7;
            break;
        
        case 0x78:
            A = B;
            cycles = 4;
            break;
        case 0x79:
            A = C;
            cycles = 4;
            break;
        case 0x7A:
            A = D;
            cycles = 4;
            break;
        case 0x7B:
            A = E;
            cycles = 4;
            break;
        case 0x7C:
            A = H;
            cycles = 4;
            break;
        case 0x7D:
            A = L;
            cycles = 4;
            break;
        case 0x7E:
            A = readMemory(HL());
            cycles = 7;
            break;
        case 0x7F:
            // A = A;
        	cycles = 4;
            break;
        
        case 0x80:
            A = cpuAdd(A, B);
            cycles = 4;
            break;
        case 0x81:
            A = cpuAdd(A, C);
            cycles = 4;
            break;
        case 0x82:
            A = cpuAdd(A, D);
            cycles = 4;
            break;
        case 0x83:
            A = cpuAdd(A, E);
            cycles = 4;
            break;
        case 0x84:
            A = cpuAdd(A, H);
            cycles = 4;
            break;
        case 0x85:
            A = cpuAdd(A, L);
            cycles = 4;
            break;
        case 0x86:
            A = cpuAdd(A, readMemory(HL()));
            cycles = 7;
            break;
        case 0x87:
            A = cpuAdd(A, A);
            cycles = 4;
            break;
        
        case 0x88:
            A = cpuAdc(A, B);
            cycles = 4;
            break;
        case 0x89:
            A = cpuAdc(A, C);
            cycles = 4;
            break;
        case 0x8A:
            A = cpuAdc(A, D);
            cycles = 4;
            break;
        case 0x8B:
            A = cpuAdc(A, E);
            cycles = 4;
            break;
        case 0x8C:
            A = cpuAdc(A, H);
            cycles = 4;
            break;
        case 0x8D:
            A = cpuAdc(A, L);
            cycles = 4;
            break;
        case 0x8E:
            A = cpuAdc(A, readMemory(HL()));
            cycles = 7;
            break;
        case 0x8F:
            A = cpuAdc(A, A);
            cycles = 4;
            break;
        
        case 0x90:
            A = cpuSub(A, B);
            cycles = 4;
            break;
        case 0x91:
            A = cpuSub(A, C);
            cycles = 4;
            break;
        case 0x92:
            A = cpuSub(A, D);
            cycles = 4;
            break;
        case 0x93:
            A = cpuSub(A, E);
            cycles = 4;
            break;
        case 0x94:
            A = cpuSub(A, H);
            cycles = 4;
            break;
        case 0x95:
            A = cpuSub(A, L);
            cycles = 4;
            break;
        case 0x96:
            A = cpuSub(A, readMemory(HL()));
            cycles = 7;
            break;
        case 0x97:
            A = cpuSub(A, A);
            cycles = 4;
            break;
            
        case 0x98:
            A = cpuSubCarry(A, B);
            cycles = 4;
            break;
        case 0x99:
            A = cpuSubCarry(A, C);
            cycles = 4;
            break;
        case 0x9A:
            A = cpuSubCarry(A, D);
            cycles = 4;
            break;
        case 0x9B:
            A = cpuSubCarry(A, E);
            cycles = 4;
            break;
        case 0x9C:
            A = cpuSubCarry(A, H);
            cycles = 4;
            break;
        case 0x9D:
            A = cpuSubCarry(A, L);
            cycles = 4;
            break;
        case 0x9E:
            A = cpuSubCarry(A, readMemory(HL()));
            cycles = 7;
            break;
        case 0x9F:
            A = cpuSubCarry(A, A);
            cycles = 4;
            break;
            
        case 0xA0:
            A = cpuAnd(A, B);
            cycles = 4;
            break;
        case 0xA1:
            A = cpuAnd(A, C);
            cycles = 4;
            break;
        case 0xA2:
            A = cpuAnd(A, D);
            cycles = 4;
            break;
        case 0xA3:
            A = cpuAnd(A, E);
            cycles = 4;
            break;
        case 0xA4:
            A = cpuAnd(A, H);
            cycles = 4;
            break;
        case 0xA5:
            A = cpuAnd(A, L);
            cycles = 4;
            break;
        case 0xA6:
            A = cpuAnd(A, readMemory(HL()));
            cycles = 7;            
            break;
        case 0xA7:
            A = cpuAnd(A, A);
            cycles = 4;
            break;
            
        case 0xA8:
            A = cpuXor(A, B);
            cycles = 4;
            break;
        case 0xA9:
            A = cpuXor(A, C);
            cycles = 4;
            break;
        case 0xAA:
            A = cpuXor(A, D);
            cycles = 4;
            break;
        case 0xAB:
            A = cpuXor(A, E);
            cycles = 4;
            break;
        case 0xAC:
            A = cpuXor(A, H);
            cycles = 4;
            break;
        case 0xAD:
            A = cpuXor(A, L);
            cycles = 4;
            break;
        case 0xAE:
            A = cpuXor(A, readMemory(HL()));
            cycles = 7;
            break;
        case 0xAF:
            A = cpuXor(A, A);
            cycles = 4;
            break;
            
        case 0xB0:
            A = cpuOr(A, B);
            cycles = 4;
            break;
        case 0xB1:
            A = cpuOr(A, C);
            cycles = 4;
            break;
        case 0xB2:
            A = cpuOr(A, D);
            cycles = 4;
            break;
        case 0xB3:
            A = cpuOr(A, E);
            cycles = 4;
            break;
        case 0xB4:
            A = cpuOr(A, H);
            cycles = 4;
            break;
        case 0xB5:
            A = cpuOr(A, L);
            cycles = 4;
            break;
        case 0xB6:
            A = cpuOr(A, readMemory(HL()));
            cycles = 7;
            break;
        case 0xB7:
            A = cpuOr(A, A);
            cycles = 4;
            break;
            
        case 0xB8:
            cpuCmp(A, B);
            cycles = 4;
            break;
        case 0xB9:
            cpuCmp(A, C);
            cycles = 4;
            break;
        case 0xBA:
            cpuCmp(A, D);
            cycles = 4;
            break;
        case 0xBB:
            cpuCmp(A, E);
            cycles = 4;
            break;
        case 0xBC:
            cpuCmp(A, H);
            cycles = 4;
            break;
        case 0xBD:
            cpuCmp(A, L);
            cycles = 4;
            break;
        case 0xBE:
            cpuCmp(A, readMemory(HL()));
            cycles = 7;
            break;
        case 0xBF:
        	cpuCmp(A, A);
        	cycles = 4;
            break;
            
        case 0xC0:  // ret nz
            cycles = cpuReturn(!isZeroFlagSet());
            break;
        case 0xC1:  // pop bc
            lo = readMemory(SP);
            hi = readMemory(SP + 1);
            tmp = (hi << 8) | lo;
            
            SP = (SP + 2) & 0xFFFF;
            BC(tmp);
            
            cycles = 10;
            break;
        case 0xC2:	//	jp nz,**
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
            
            if (!isZeroFlagSet()) {
                PC = tmp;
            } else {
                PC = (PC + 2) & 0xFFFF;
            }
            cycles = 10;
            
            break;
        case 0xC3:  // jp **
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            
            tmp = (hi << 8) | lo;
            PC = tmp;
            
            cycles = 10;
            
            break;
        case 0xC4:  // call nz,**
            cycles = call(!isZeroFlagSet());
            break;
        case 0xC5:	//	push bc
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, B);
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, C);
            
            cycles = 11;
            break;
        case 0xC6:  // add a,*
            tmp = readMemory(PC);
            A = cpuAdd(A, tmp);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0xC7:  // rst 00h
            cpuRestart(0x00);
            cycles = 11;
            break;
        case 0xC8:	//	ret z
            cycles = cpuReturn(isZeroFlagSet());
            break;
        case 0xC9:	//	ret
            lo = readMemory(SP);
            hi = readMemory(SP + 1);
            tmp = (hi << 8) | lo;
      
            SP = (SP + 2) & 0xFFFF;
            PC = tmp;
            cycles = 10;
            break;
        case 0xCA:	//	jp z,**
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
            
            if (isZeroFlagSet()) {
                PC = tmp;
            } else {
                PC = (PC + 2) & 0xFFFF;
            }
            
            cycles = 10;
            break;
        case 0xCB:
        	cycles = extendedCB();
        	break;
        case 0xCC:  // call z,**
        	cycles = call(isZeroFlagSet());
            break;
        case 0xCD:  // call **
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            
            tmp = (hi << 8) | lo;
            
            PC = (PC + 2) & 0xFFFF;
            hi = PC >> 8;
            lo = PC & 0xFF;

            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, hi);
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, lo);
            
            PC = tmp;
            cycles = 17;
            
            break;
        case 0xCE:  // adc a,*
            tmp = readMemory(PC);
            A = cpuAdc(A, tmp);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0xCF:  // rst 08h
            cpuRestart(0x08);
            cycles = 11;
            break;
        case 0xD0:  // ret nc
        	cycles = cpuReturn(!isCarryFlagSet());
            break;
        case 0xD1:  // pop de
        	lo = readMemory(SP);
            hi = readMemory(SP + 1);
            tmp = (hi << 8) | lo;
            
            SP = (SP + 2) & 0xFFFF;
            DE(tmp);
            
            cycles = 10;
            break;
        case 0xD2:  // jp nc,**
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
            
            if (!isCarryFlagSet()) {
                PC = tmp;
            } else {
                PC = (PC + 2) & 0xFFFF;
            }
            
            cycles = 10;
            break;
        //	OUT (n), A
        case 0xD3:
        	tmp = readMemory(PC);
        	bus.outPort(tmp, A);
        	
        	PC = (PC + 1) & 0xFFFF;
        	cycles = 11;
        	break;
        case 0xD4:	//	call nc,**
        	cycles = call(!isCarryFlagSet());
            break;
        case 0xD5:	//	push de
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, D);
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, E);
            
            cycles = 11;
            break;
        case 0xD6:  //	sub *
            tmp = readMemory(PC);
            A = cpuSub(A, tmp);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0xD7:  //	rst 10h
            cpuRestart(0x10);
            cycles = 11;
            break;
        case 0xD8:  //	ret c
        	cycles = cpuReturn(isCarryFlagSet());
            break;
        case 0xD9:  // EXX
        	tmp = BC();
        	BC(BC2());
        	BC2(tmp);
        	
        	tmp = DE();
        	DE(DE2());
        	DE2(tmp);
        	
        	tmp = HL();
        	HL(HL2());
        	HL2(tmp);
        	
        	cycles = 4;
        	break;
        case 0xDA:  // jp c,**
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
            
            if (isCarryFlagSet()) {
                PC = tmp;
            } else {
                PC = (PC + 2) & 0xFFFF;
            }
            
            cycles = 10;
            break;
       // IN 	a, (n)
        case 0xDB:
        	tmp = readMemory(PC);
        	A = bus.inPort(tmp);
        	
        	PC = (PC + 1) & 0xFFFF;
        	cycles = 11;
        	break;
        case 0xDC:  // call c,**
        	cycles = call(isCarryFlagSet());
            break;
        case 0xDD:
        	cycles = extendedDD();
        	break;
        case 0xDE:  // sbc a,*
            tmp = readMemory(PC);
            A = cpuSubCarry(A, tmp);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0xDF:  // rst 18h
            cpuRestart(0x18);
            cycles = 11;
            break;
        case 0xE0:  // ret po
        	cycles = cpuReturn(!isParityFlagSet());
            break;
        case 0xE1:  // pop hl
        	lo = readMemory(SP);
            hi = readMemory(SP + 1);
            tmp = (hi << 8) | lo;
            
            SP = (SP + 2) & 0xFFFF;
            HL(tmp);
            
            cycles = 10;
            break;
        case 0xE2:  // jp po,xx
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
            
            if (!isParityFlagSet()) {
                PC = tmp;
            } else {
                PC = (PC + 2) & 0xFFFF;
            }
            
            cycles = 10;
            break;
        //	EX (SP),HL exchanges HL with the last pushed value on the stack.
        case 0xE3:
        	int tmpH = H;
        	int tmpL = L;
        	L = readMemory(SP);
        	H = readMemory(SP + 1);
        	writeMemory(SP, tmpL);
        	writeMemory(SP + 1, tmpH);
        	
        	cycles = 19;
        	break;
        	
        case 0xE4:	//	call po,**
        	cycles = call(!isParityFlagSet());
            break;
        case 0xE5:  // push hl
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, H);
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, L);

            cycles = 11;
            break;
        case 0xE6:  // and *
            A = cpuAnd(A, readMemory(PC));
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0xE7:  // rst 20h
            cpuRestart(0x20);
            cycles = 11;
            break;
        case 0xE8:	//	ret pe
        	cycles = cpuReturn(isParityFlagSet());
            break;
        case 0xE9:	//	jp (hl)
            PC = HL();
            cycles = 4;
            break;
        case 0xEA:  // jp pe,xx
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
            
            if (isParityFlagSet()) {
                PC = tmp;
            } else {
                PC = (PC + 2) & 0xFFFF;
            }
            
            cycles = 10;
            break;
            
        //	The 16 bits of data held in the Hand L registers are exchanged with the 16 bits of data
        //	held in the D and E registers.
        case 0xEB:
        	tmp = DE();
        	DE(HL());
        	HL(tmp);
        	
        	cycles = 4;
        	break;
        case 0xEC:	//	call pe,**
        	cycles = call(isParityFlagSet());
            break;
        case 0xED:
        	cycles = extendedED(opcode);
        	break;
        case 0xEE:  // xor *
            tmp = readMemory(PC);
            A = cpuXor(A, tmp);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0xEF:  // rst 28h
            cpuRestart(0x28);
            cycles = 11;
            break;
        case 0xF0:  // ret p	positive (sign)
        	cycles = cpuReturn(!isSignFlagSet());
            break;
        case 0xF1:  // pop af
            lo = readMemory(SP);
            hi = readMemory(SP + 1);
            tmp = (hi << 8) | lo;
            
            SP = (SP + 2) & 0xFFFF;
            AF(tmp);
            
            cycles = 10;
            
            break;
        case 0xF2:  // jp p,xx
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
            
            if (!isSignFlagSet()) {
                PC = tmp;
            } else {
                PC = (PC + 2) & 0xFFFF;
            }
            
            cycles = 10;
            break;
        case 0xF3:	//	DI
            disableInterruptsNow = true;
            enableInterrupts = false;
            cycles = 4;
            break;
        case 0xF4:	//	call p,**
        	cycles = call(!isSignFlagSet());
            break;
        case 0xF5:  // push af
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, A);
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, F);

            cycles = 11;
            break;
        case 0xF6:  // or *
            tmp = readMemory(PC);
            A = cpuOr(A, tmp);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0xF7:  // rst 30h
            cpuRestart(0x30);
            cycles = 11;
            break;
        case 0xF8:  // ret m	minus (sign)
        	cycles = cpuReturn(isSignFlagSet());
            break;
        case 0xF9:	//	ld sp,hl
            SP = HL();
            cycles = 6;
            break;
        case 0xFA:  // jp m,xx
            lo = readMemory(PC);
            hi = readMemory(PC + 1);
            tmp = (hi << 8) | lo;
            
            if (isSignFlagSet()) {
                PC = tmp;
            } else {
                PC = (PC + 2) & 0xFFFF;
            }
            
            cycles = 10;
            break;
        case 0xFB:	// EI
            enableInterruptsNextInstr = true;
            cycles = 4;
            break;
        case 0xFC:	//	call m,**
        	cycles = call(isSignFlagSet());
            break;
        case 0xFD:
        	cycles = extendedFD();
        	break;
        case 0xFE:  // cp *
            tmp = readMemory(PC);
            cpuCmp(A, tmp);
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 7;
            break;
        case 0xFF:  // rst 38h
            cpuRestart(0x38);
            cycles = 11;
            break;
            
        default:
        	for (int i = 0; i < lastInstr.length; i++) {
				System.out.println(lastInstr[i]);
			}
            throw new RuntimeException("Illegal opcode " + hex(opcode) + " found at pc: " + hex4(PC - 1));
        }
        
        // TODO emular bien esto, en las instrucciones con while aumenta mas (creo que aumenta por ciclos ejecutados)
        R = (R + 1) & 0x7F;
		
		return cycles;
    }

	private int extendedCB() {
		int cycles = 0;
		int tmp;
		
		int extOpcode = readMemory(PC);
		
		switch (extOpcode) {
		case 0x00:
			B = cpuRlc(B);
			cycles = 8;
			break;
		case 0x01:
			C = cpuRlc(C);
			cycles = 8;
			break;
		case 0x02:
			D = cpuRlc(D);
			cycles = 8;
			break;
		case 0x03:
			E = cpuRlc(E);
			cycles = 8;
			break;
		case 0x04:
			H = cpuRlc(H);
			cycles = 8;
			break;
		case 0x05:
			L = cpuRlc(L);
			cycles = 8;
			break;
		case 0x06:
			tmp = cpuRlc(readMemory(HL()));
			writeMemory(HL(), tmp);
			cycles = 15;
			break;
		case 0x07:
			A = cpuRlc(A);
			cycles = 8;
			break;
			
		case 0x08:
			B = cpuRrc(B);
			cycles = 8;
			break;
		case 0x09:
			C = cpuRrc(C);
			cycles = 8;
			break;
		case 0x0A:
			D = cpuRrc(D);
			cycles = 8;
			break;
		case 0x0B:
			E = cpuRrc(E);
			cycles = 8;
			break;
		case 0x0C:
			H = cpuRrc(H);
			cycles = 8;
			break;
		case 0x0D:
			L = cpuRrc(L);
			cycles = 8;
			break;
		case 0x0E:
			tmp = cpuRrc(readMemory(HL()));
			writeMemory(HL(), tmp);
			cycles = 15;
			break;
		case 0x0F:
			A = cpuRrc(A);
			cycles = 8;
			break;
		
		case 0x10:
			B = cpuRl(B);
			cycles = 8;
			break;
		case 0x11:
			C = cpuRl(C);
			cycles = 8;
			break;
		case 0x12:
			D = cpuRl(D);
			cycles = 8;
			break;
		case 0x13:
			E = cpuRl(E);
			cycles = 8;
			break;
		case 0x14:
			H = cpuRl(H);
			cycles = 8;
			break;
		case 0x15:
			L = cpuRl(L);
			cycles = 8;
			break;
		case 0x16:
			tmp = cpuRl(readMemory(HL()));
			writeMemory(HL(), tmp);
			cycles = 15;
			break;
		case 0x17:
			A = cpuRl(A);
			cycles = 8;
			break;
			
		case 0x18:
			B = cpuRr(B);
			cycles = 8;
			break;
		case 0x19:
			C = cpuRr(C);
			cycles = 8;
			break;
		case 0x1A:
			D = cpuRr(D);
			cycles = 8;
			break;
		case 0x1B:
			E = cpuRr(E);
			cycles = 8;
			break;
		case 0x1C:
			H = cpuRr(H);
			cycles = 8;
			break;
		case 0x1D:
			L = cpuRr(L);
			cycles = 8;
			break;
		case 0x1E:
			tmp = cpuRr(readMemory(HL()));
			writeMemory(HL(), tmp);
			cycles = 15;
			break;
		case 0x1F:
			A = cpuRr(A);
			cycles = 8;
			break;
			
		case 0x20:
			B = cpuSla(B);
			cycles = 8;
			break;
		case 0x21:
			C = cpuSla(C);
			cycles = 8;
			break;
		case 0x22:
			D = cpuSla(D);
			cycles = 8;
			break;
		case 0x23:
			E = cpuSla(E);
			cycles = 8;
			break;
		case 0x24:
			H = cpuSla(H);
			cycles = 8;
			break;
		case 0x25:
			L = cpuSla(L);
			cycles = 8;
			break;
		case 0x26:
			tmp = cpuSla(readMemory(HL()));
			writeMemory(HL(), tmp);
			cycles = 15;
			break;
		case 0x27:
			A = cpuSla(A);
			cycles = 8;
			break;

		case 0x28:
			B = cpuSra(B);
			cycles = 8;
			break;
		case 0x29:
			C = cpuSra(C);
			cycles = 8;
			break;
		case 0x2A:
			D = cpuSra(D);
			cycles = 8;
			break;
		case 0x2B:
			E = cpuSra(E);
			cycles = 8;
			break;
		case 0x2C:
			H = cpuSra(H);
			cycles = 8;
			break;
		case 0x2D:
			L = cpuSra(L);
			cycles = 8;
			break;
		case 0x2E:
			tmp = cpuSra(readMemory(HL()));
			writeMemory(HL(), tmp);
			cycles = 15;
			break;
		case 0x2F:
			A = cpuSra(A);
			cycles = 8;
			break;
			
		case 0x30:
			B = cpuSll(B);
			cycles = 8;
			break;
		case 0x31:
			C = cpuSll(C);
			cycles = 8;
			break;
		case 0x32:
			D = cpuSll(D);
			cycles = 8;
			break;
		case 0x33:
			E = cpuSll(E);
			cycles = 8;
			break;
		case 0x34:
			H = cpuSll(H);
			cycles = 8;
			break;
		case 0x35:
			L = cpuSll(L);
			cycles = 8;
			break;
		case 0x36:
			tmp = cpuSll(readMemory(HL()));
			writeMemory(HL(), tmp);
			cycles = 15;
			break;
		case 0x37:
			A = cpuSll(A);
			cycles = 8;
			break;
			
		case 0x38:
			B = cpuSrl(B);
			cycles = 8;
			break;
		case 0x39:
			C = cpuSrl(C);
			cycles = 8;
			break;
		case 0x3A:
			D = cpuSrl(D);
			cycles = 8;
			break;
		case 0x3B:
			E = cpuSrl(E);
			cycles = 8;
			break;
		case 0x3C:
			H = cpuSrl(H);
			cycles = 8;
			break;
		case 0x3D:
			L = cpuSrl(L);
			cycles = 8;
			break;
		case 0x3E:
			tmp = cpuSrl(readMemory(HL()));
			writeMemory(HL(), tmp);
			cycles = 15;
			break;
		case 0x3F:
			A = cpuSrl(A);
			cycles = 8;
			break;
			
		case 0x40:
			cpuTestBit(B, 0);
			cycles = 8;
			break;
		case 0x41:
			cpuTestBit(C, 0);
			cycles = 8;
			break;
		case 0x42:
			cpuTestBit(D, 0);
			cycles = 8;
			break;
		case 0x43:
			cpuTestBit(E, 0);
			cycles = 8;
			break;
		case 0x44:
			cpuTestBit(H, 0);
			cycles = 8;
			break;
		case 0x45:
			cpuTestBit(L, 0);
			cycles = 8;
			break;
		case 0x46:
			tmp = readMemory(HL());
			cpuTestBit(tmp, 0);
			cycles = 12;
			break;
		case 0x47:
			cpuTestBit(A, 0);
			cycles = 8;
			break;
			
		case 0x48:
			cpuTestBit(B, 1);
			cycles = 8;
			break;
		case 0x49:
			cpuTestBit(C, 1);
			cycles = 8;
			break;
		case 0x4A:
			cpuTestBit(D, 1);
			cycles = 8;
			break;
		case 0x4B:
			cpuTestBit(E, 1);
			cycles = 8;
			break;
		case 0x4C:
			cpuTestBit(H, 1);
			cycles = 8;
			break;
		case 0x4D:
			cpuTestBit(L, 1);
			cycles = 8;
			break;
		case 0x4E:
			tmp = readMemory(HL());
			cpuTestBit(tmp, 1);
			cycles = 12;
			break;
		case 0x4F:
			cpuTestBit(A, 1);
			cycles = 8;
			break;
			
		case 0x50:
			cpuTestBit(B, 2);
			cycles = 8;
			break;
		case 0x51:
			cpuTestBit(C, 2);
			cycles = 8;
			break;
		case 0x52:
			cpuTestBit(D, 2);
			cycles = 8;
			break;
		case 0x53:
			cpuTestBit(E, 2);
			cycles = 8;
			break;
		case 0x54:
			cpuTestBit(H, 2);
			cycles = 8;
			break;
		case 0x55:
			cpuTestBit(L, 2);
			cycles = 8;
			break;
		case 0x56:
			tmp = readMemory(HL());
			cpuTestBit(tmp, 2);
			cycles = 12;
			break;
		case 0x57:
			cpuTestBit(A, 2);
			cycles = 8;
			break;
		
		case 0x58:
			cpuTestBit(B, 3);
			cycles = 8;
			break;
		case 0x59:
			cpuTestBit(C, 3);
			cycles = 8;
			break;
		case 0x5A:
			cpuTestBit(D, 3);
			cycles = 8;
			break;
		case 0x5B:
			cpuTestBit(E, 3);
			cycles = 8;
			break;
		case 0x5C:
			cpuTestBit(H, 3);
			cycles = 8;
			break;
		case 0x5D:
			cpuTestBit(L, 3);
			cycles = 8;
			break;
		case 0x5E:
			tmp = readMemory(HL());
			cpuTestBit(tmp, 3);
			cycles = 12;
			break;
		case 0x5F:
			cpuTestBit(A, 3);
			cycles = 8;
			break;
			
		case 0x60:
			cpuTestBit(B, 4);
			cycles = 8;
			break;
		case 0x61:
			cpuTestBit(C, 4);
			cycles = 8;
			break;
		case 0x62:
			cpuTestBit(D, 4);
			cycles = 8;
			break;
		case 0x63:
			cpuTestBit(E, 4);
			cycles = 8;
			break;
		case 0x64:
			cpuTestBit(H, 4);
			cycles = 8;
			break;
		case 0x65:
			cpuTestBit(L, 4);
			cycles = 8;
			break;
		case 0x66:
			tmp = readMemory(HL());
			cpuTestBit(tmp, 4);
			cycles = 12;
			break;
		case 0x67:
			cpuTestBit(A, 4);
			cycles = 8;
			break;
			
		case 0x68:
			cpuTestBit(B, 5);
			cycles = 8;
			break;
		case 0x69:
			cpuTestBit(C, 5);
			cycles = 8;
			break;
		case 0x6A:
			cpuTestBit(D, 5);
			cycles = 8;
			break;
		case 0x6B:
			cpuTestBit(E, 5);
			break;
		case 0x6C:
			cpuTestBit(H, 5);
			cycles = 8;
			break;
		case 0x6D:
			cpuTestBit(L, 5);
			cycles = 8;
			break;
		case 0x6E:
			tmp = readMemory(HL());
			cpuTestBit(tmp, 5);
			cycles = 12;
			break;
		case 0x6F:
			cpuTestBit(A, 5);
			cycles = 8;
			break;
			
		case 0x70:
			cpuTestBit(B, 6);
			cycles = 8;
			break;
		case 0x71:
			cpuTestBit(C, 6);
			cycles = 8;
			break;
		case 0x72:
			cpuTestBit(D, 6);
			cycles = 8;
			break;
		case 0x73:
			cpuTestBit(E, 6);
			cycles = 8;
			break;
		case 0x74:
			cpuTestBit(H, 6);
			cycles = 8;
			break;
		case 0x75:
			cpuTestBit(L, 6);
			cycles = 8;
			break;
		case 0x76:
			tmp = readMemory(HL());
			cpuTestBit(tmp, 6);
			cycles = 12;
			break;
		case 0x77:
			cpuTestBit(A, 6);
			cycles = 8;
			break;
			
		case 0x78:
			cpuTestBit(B, 7);
			cycles = 8;
			break;
		case 0x79:
			cpuTestBit(C, 7);
			cycles = 8;
			break;
		case 0x7A:
			cpuTestBit(D, 7);
			cycles = 8;
			break;
		case 0x7B:
			cpuTestBit(E, 7);
			cycles = 8;
			break;
		case 0x7C:
			cpuTestBit(H, 7);
			cycles = 8;
			break;
		case 0x7D:
			cpuTestBit(L, 7);
			cycles = 8;
			break;
		case 0x7E:
			tmp = readMemory(HL());
			cpuTestBit(tmp, 7);
			cycles = 12;
			break;
		case 0x7F:
			cpuTestBit(A, 7);
			cycles = 8;
			break;
			
		case 0x80:
			B = bitReset(B, 0);
			cycles = 8;
			break;
		case 0x81:
			C = bitReset(C, 0);
			cycles = 8;
			break;
		case 0x82:
			D = bitReset(D, 0);
			cycles = 8;
			break;
		case 0x83:
			E = bitReset(E, 0);
			cycles = 8;
			break;
		case 0x84:
			H = bitReset(H, 0);
			cycles = 8;
			break;
		case 0x85:
			L = bitReset(L, 0);
			cycles = 8;
			break;
		case 0x86:
			tmp = bitReset(readMemory(HL()), 0);
            writeMemory(HL(), tmp);
            cycles = 15;
			break;
		case 0x87:
			A = bitReset(A, 0);
			cycles = 8;
			break;

		case 0x88:
			B = bitReset(B, 1);
			cycles = 8;
			break;
		case 0x89:
			C = bitReset(C, 1);
			cycles = 8;
			break;
		case 0x8A:
			D = bitReset(D, 1);
			cycles = 8;
			break;
		case 0x8B:
			E = bitReset(E, 1);
			cycles = 8;
			break;
		case 0x8C:
			H = bitReset(H, 1);
			cycles = 8;
			break;
		case 0x8D:
			L = bitReset(L, 1);
			cycles = 8;
			break;
		case 0x8E:
			tmp = bitReset(readMemory(HL()), 1);
            writeMemory(HL(), tmp);
            cycles = 15;
			break;
		case 0x8F:
			A = bitReset(A, 1);
			cycles = 8;
			break;
			
		case 0x90:
			B = bitReset(B, 2);
			cycles = 8;
			break;
		case 0x91:
			C = bitReset(C, 2);
			cycles = 8;
			break;
		case 0x92:
			D = bitReset(D, 2);
			cycles = 8;
			break;
		case 0x93:
			E = bitReset(E, 2);
			cycles = 8;
			break;
		case 0x94:
			H = bitReset(H, 2);
			cycles = 8;
			break;
		case 0x95:
			L = bitReset(L, 2);
			cycles = 8;
			break;
		case 0x96:
			tmp = bitReset(readMemory(HL()), 2);
            writeMemory(HL(), tmp);
            cycles = 15;
			break;
		case 0x97:
			A = bitReset(A, 2);
			cycles = 8;
			break;
		
		case 0x98:
			B = bitReset(B, 3);
			cycles = 8;
			break;
		case 0x99:
			C = bitReset(C, 3);
			cycles = 8;
			break;
		case 0x9A:
			D = bitReset(D, 3);
			cycles = 8;
			break;
		case 0x9B:
			E = bitReset(E, 3);
			cycles = 8;
			break;
		case 0x9C:
			H = bitReset(H, 3);
			cycles = 8;
			break;
		case 0x9D:
			L = bitReset(L, 3);
			cycles = 8;
			break;
		case 0x9E:
			tmp = bitReset(readMemory(HL()), 3);
            writeMemory(HL(), tmp);
            cycles = 15;
			break;
		case 0x9F:
			A = bitReset(A, 3);
			cycles = 8;
			break;
			
		case 0xA0:
			B = bitReset(B, 4);
			cycles = 8;
			break;
		case 0xA1:
			C = bitReset(C, 4);
			cycles = 8;
			break;
		case 0xA2:
			D = bitReset(D, 4);
			cycles = 8;
			break;
		case 0xA3:
			E = bitReset(E, 4);
			cycles = 8;
			break;
		case 0xA4:
			H = bitReset(H, 4);
			cycles = 8;
			break;
		case 0xA5:
			L = bitReset(L, 4);
			cycles = 8;
			break;
		case 0xA6:
			tmp = bitReset(readMemory(HL()), 4);
            writeMemory(HL(), tmp);
            cycles = 15;
			break;
		case 0xA7:
			A = bitReset(A, 4);
			cycles = 8;
			break;
			
		case 0xA8:
			B = bitReset(B, 5);
			cycles = 8;
			break;
		case 0xA9:
			C = bitReset(C, 5);
			cycles = 8;
			break;
		case 0xAA:
			D = bitReset(D, 5);
			cycles = 8;
			break;
		case 0xAB:
			E = bitReset(E, 5);
			cycles = 8;
			break;
		case 0xAC:
			H = bitReset(H, 5);
			cycles = 8;
			break;
		case 0xAD:
			L = bitReset(L, 5);
			cycles = 8;
			break;
		case 0xAE:
			tmp = bitReset(readMemory(HL()), 5);
            writeMemory(HL(), tmp);
            cycles = 15;
			break;
		case 0xAF:
			A = bitReset(A, 5);
			cycles = 8;
			break;
			
		case 0xB0:
			B = bitReset(B, 6);
			cycles = 8;
			break;
		case 0xB1:
			C = bitReset(C, 6);
			cycles = 8;
			break;
		case 0xB2:
			D = bitReset(D, 6);
			cycles = 8;
			break;
		case 0xB3:
			E = bitReset(E, 6);
			cycles = 8;
			break;
		case 0xB4:
			H = bitReset(H, 6);
			cycles = 8;
			break;
		case 0xB5:
			L = bitReset(L, 6);
			cycles = 8;
			break;
		case 0xB6:
			tmp = bitReset(readMemory(HL()), 6);
            writeMemory(HL(), tmp);
            cycles = 15;
			break;
		case 0xB7:
			A = bitReset(A, 6);
			cycles = 8;
			break;
			
		case 0xB8:
			B = bitReset(B, 7);
			cycles = 8;
			break;
		case 0xB9:
			C = bitReset(C, 7);
			cycles = 8;
			break;
		case 0xBA:
			D = bitReset(D, 7);
			cycles = 8;
			break;
		case 0xBB:
			E = bitReset(E, 7);
			cycles = 8;
			break;
		case 0xBC:
			H = bitReset(H, 7);
			cycles = 8;
			break;
		case 0xBD:
			L = bitReset(L, 7);
			cycles = 8;
			break;
		case 0xBE:
			tmp = bitReset(readMemory(HL()), 7);
            writeMemory(HL(), tmp);
            cycles = 15;
			break;
		case 0xBF:
			A = bitReset(A, 7);
			cycles = 8;
			break;
			
		case 0xC0:
            B = bitSet(B, 0);
            cycles = 8;
            break;
        case 0xC1:
            C = bitSet(C, 0);
            cycles = 8;
            break;
        case 0xC2:
            D = bitSet(D, 0);
            cycles = 8;
            break;
        case 0xC3:
            E = bitSet(E, 0);
            cycles = 8;
            break;
        case 0xC4:
            H = bitSet(H, 0);
            cycles = 8;
            break;
        case 0xC5:
            L = bitSet(L, 0);
            cycles = 8;
            break;
        case 0xC6:
            tmp = bitSet(readMemory(HL()), 0);
            writeMemory(HL(), tmp);
            cycles = 15;
            break;
        case 0xC7:
            A = bitSet(A, 0);
            cycles = 8;
            break;
			
        case 0xC8:
            B = bitSet(B, 1);
            cycles = 8;
            break;
        case 0xC9:
            C = bitSet(C, 1);
            cycles = 8;
            break;
        case 0xCA:
            D = bitSet(D, 1);
            cycles = 8;
            break;
        case 0xCB:
            E = bitSet(E, 1);
            cycles = 8;
            break;
        case 0xCC:
            H = bitSet(H, 1);
            cycles = 8;
            break;
        case 0xCD:
            L = bitSet(L, 1);
            cycles = 8;
            break;
        case 0xCE:
            tmp = bitSet(readMemory(HL()), 1);
            writeMemory(HL(), tmp);
            cycles = 15;
            break;
        case 0xCF:
            A = bitSet(A, 1);
            cycles = 8;
            break;
          
        case 0xD0:
            B = bitSet(B, 2);
            cycles = 8;
            break;
        case 0xD1:
            C = bitSet(C, 2);
            cycles = 8;
            break;
        case 0xD2:
            D = bitSet(D, 2);
            cycles = 8;
            break;
        case 0xD3:
            E = bitSet(E, 2);
            cycles = 8;
            break;
        case 0xD4:
            H = bitSet(H, 2);
            cycles = 8;
            break;
        case 0xD5:
            L = bitSet(L, 2);
            cycles = 8;
            break;
        case 0xD6:
            tmp = bitSet(readMemory(HL()), 2);
            writeMemory(HL(), tmp);
            cycles = 15;
            break;
        case 0xD7:
            A = bitSet(A, 2);
            cycles = 8;
            break;
            
        case 0xD8:
            B = bitSet(B, 3);
            cycles = 8;
            break;
        case 0xD9:
            C = bitSet(C, 3);
            cycles = 8;
            break;
        case 0xDA:
            D = bitSet(D, 3);
            cycles = 8;
            break;
        case 0xDB:
            E = bitSet(E, 3);
            cycles = 8;
            break;
        case 0xDC:
            H = bitSet(H, 3);
            cycles = 8;
            break;
        case 0xDD:
            L = bitSet(L, 3);
            cycles = 8;
            break;
        case 0xDE:
            tmp = bitSet(readMemory(HL()), 3);
            writeMemory(HL(), tmp);
            cycles = 15;
            break;
        case 0xDF:
            A = bitSet(A, 3);
            cycles = 8;
            break;
            
        case 0xE0:
            B = bitSet(B, 4);
            cycles = 8;
            break;
        case 0xE1:
            C = bitSet(C, 4);
            cycles = 8;
            break;
        case 0xE2:
            D = bitSet(D, 4);
            cycles = 8;
            break;
        case 0xE3:
            E = bitSet(E, 4);
            cycles = 8;
            break;
        case 0xE4:
            H = bitSet(H, 4);
            cycles = 8;
            break;
        case 0xE5:
            L = bitSet(L, 4);
            cycles = 8;
            break;
        case 0xE6:
            tmp = bitSet(readMemory(HL()), 4);
            writeMemory(HL(), tmp);
            cycles = 15;
            break;
        case 0xE7:
            A = bitSet(A, 4);
            cycles = 8;
            break;
            
        case 0xE8:
            B = bitSet(B, 5);
            cycles = 8;
            break;
        case 0xE9:
            C = bitSet(C, 5);
            cycles = 8;
            break;
        case 0xEA:
            D = bitSet(D, 5);
            cycles = 8;
            break;
        case 0xEB:
            E = bitSet(E, 5);
            cycles = 8;
            break;
        case 0xEC:
            H = bitSet(H, 5);
            cycles = 8;
            break;
        case 0xED:
            L = bitSet(L, 5);
            cycles = 8;
            break;
        case 0xEE:
            tmp = bitSet(readMemory(HL()), 5);
            writeMemory(HL(), tmp);
            cycles = 15;
            break;
        case 0xEF:
            A = bitSet(A, 5);
            cycles = 8;
            break;
            
        case 0xF0:
            B = bitSet(B, 6);
            cycles = 8;
            break;
        case 0xF1:
            C = bitSet(C, 6);
            cycles = 8;
            break;
        case 0xF2:
            D = bitSet(D, 6);
            cycles = 8;
            break;
        case 0xF3:
            E = bitSet(E, 6);
            cycles = 8;
            break;
        case 0xF4:
            H = bitSet(H, 6);
            cycles = 8;
            break;
        case 0xF5:
            L = bitSet(L, 6);
            cycles = 8;
            break;
        case 0xF6:
            tmp = bitSet(readMemory(HL()), 6);
            writeMemory(HL(), tmp);
            cycles = 15;
            break;
        case 0xF7:
            A = bitSet(A, 6);
            cycles = 8;
            break;
            
        case 0xF8:
            B = bitSet(B, 7);
            cycles = 8;
            break;
        case 0xF9:
            C = bitSet(C, 7);
            cycles = 8;
            break;
        case 0xFA:
            D = bitSet(D, 7);
            cycles = 8;
            break;
        case 0xFB:
            E = bitSet(E, 7);
            cycles = 8;
            break;
        case 0xFC:
            H = bitSet(H, 7);
            cycles = 8;
            break;
        case 0xFD:
            L = bitSet(L, 7);
            cycles = 8;
            break;
        case 0xFE:
            tmp = bitSet(readMemory(HL()), 7);
            writeMemory(HL(), tmp);
            cycles = 15;
            break;
        case 0xFF:
            A = bitSet(A, 7);
            cycles = 8;
            break;
			
			default:
				throw new RuntimeException("Illegal opcode CB " + hex(extOpcode) + " found at pc: " + hex4(PC - 1));
		}
		
		PC = (PC + 1) & 0xFFFF;
		
		return cycles;
	}

	private int extendedDD() {
		int tmp, hi, lo, offset, data;
		int addr;
		int cycles = 0;
		int extOpcode = readMemory(PC);
		switch (extOpcode) {
		case 0x09:	//	add ix,bc
			tmp = add16Bits(IX, BC());
			IX = tmp;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		case 0x19:	//	add ix,de
			tmp = add16Bits(IX, DE());
			IX = tmp;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		case 0x21:	// LD IX, **
			lo = readMemory(PC + 1);
            hi = readMemory(PC + 2);
            tmp = (hi << 8) | lo;
			
            IX = tmp;
            
            PC = (PC + 3) & 0xFFFF;
			cycles = 14;
			break;
		//	ld (**),ix
		case 0x22:
			lo = readMemory(PC + 1);
            hi = readMemory(PC + 2);
            addr = (hi << 8) | lo;

            writeMemory(addr, (IX & 0xFF));
            writeMemory(addr + 1, (IX >> 8));
			
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			break;
		//	inc ix
		case 0x23:
			IX = (IX + 1) & 0xFFFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 10;
			break;
		//	inc ixh		unofficial
		case 0x24:
			tmp = (IX >> 8);
			tmp = cpuInc(tmp);
			IX = (tmp << 8) | (IX & 0x00FF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	dec ixh		unofficial
		case 0x25:
			tmp = (IX >> 8);
			tmp = cpuDec(tmp);
			IX = (tmp << 8) | (IX & 0x00FF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
			
		//	ld ixh,*	Unofficial
		case 0x26:
			data = readMemory(PC + 1);
            
			IX = (data << 8) | (IX & 0x00FF);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 11;
			break;
		//	add ix,ix
		case 0x29:
			tmp = add16Bits(IX, IX);
			IX = tmp;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		// ld ix,**
		case 0x2A:
			lo = readMemory(PC + 1);
            hi = readMemory(PC + 2);
            addr = (hi << 8) | lo;
            
            lo = readMemory(addr);
            hi = readMemory(addr + 1);
			
            IX = (hi << 8) | lo;
			
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			break;
		//	dec ix
		case 0x2B:
			IX = (IX - 1) & 0xFFFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 10;
			break;
			
		//	inc ixl
		case 0x2C:
			tmp = (IX & 0xFF);
			tmp = cpuInc(tmp);
			IX = (IX & 0xFF00) | (tmp);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	dec ixl
		case 0x2D:
			tmp = (IX & 0xFF);
			tmp = cpuDec(tmp);
			IX = (IX & 0xFF00) | (tmp);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		
		//	ld ixl,*	Unofficial
		case 0x2E:
			data = readMemory(PC + 1);
			
			IX = (IX & 0xFF00) | data;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 11;
			break;
			
		//	inc (ix+*)
		case 0x34:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			data = readMemory(addr);
			
			data = cpuInc(data);
			writeMemory(addr, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 23;
			break;
		//	dec (ix+*)
		case 0x35:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			data = readMemory(addr);
			
			data = cpuDec(data);
			writeMemory(addr, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 23;
			break;
		//	ld(ix+*),*
		case 0x36:
			offset = readMemory(PC + 1);
			data = readMemory(PC + 2);
			addr = (IX + offset) & 0xFFFF;
			
			writeMemory(addr, data);
			
			PC = (PC + 3) & 0xFFFF;
			cycles = 19;
			break;
			
		case 0x39:	//	add ix,sp
			tmp = add16Bits(IX, SP);
			IX = tmp;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		case 0x44:	//	ld b,ixh
			B = IX >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
        case 0x45:	//	ld b,ixl
			B = IX & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld b,(ix+*)
		case 0x46:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			B = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x4C:	//	ld c,ixh
			C = IX >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
        case 0x4D:	//	ld c,ixl
			C = IX & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld c,(ix+*)
		case 0x4E:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			C = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
        case 0x54:	//	ld d,ixh
			D = IX >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
        case 0x55:	//	ld d,ixl
			D = IX & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld d,(ix+*)
		case 0x56:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			D = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x5C:	//	ld e,ixh
			E = IX >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
        case 0x5D:	//	ld e,ixl
			E = IX & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld e,(ix+*)
		case 0x5E:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			E = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x60:	//	ld ixh,b
			IX = (B << 8) | (IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x61:	//	ld ixh,c
			IX = (C << 8) | (IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x62:	//	ld ixh,d
			IX = (D << 8) | (IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x63:	//	ld ixh,e
			IX = (E << 8) | (IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x64:	//	ld ixh,ixh		(no hace nada)
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x65:	//	ld ixh,ixl
			IX = ((IX & 0xFF) << 8) | (IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld h,(ix+*)
		case 0x66:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			H = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x67:	//	ld ixh,a
			IX = (A << 8) | (IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x68:	//	ld ixl,b
			IX = (IX & 0xFF00) | B;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x69:	//	ld ixl,c
			IX = (IX & 0xFF00) | C;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x6A:	//	ld ixl,d
			IX = (IX & 0xFF00) | D;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x6B:	//	ld ixl,e
			IX = (IX & 0xFF00) | E;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x6C:	//	ld ixl,ixh
			IX = (IX & 0xFF00) | ((IX & 0xFF00) >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x6D:	//	ld ixl,ixl		no hace nada
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld l,(ix+*)
		case 0x6E:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			L = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x6F:	//	ld ixl,a
			IX = (IX & 0xFF00) | A;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	ld (ix+*),b
		case 0x70:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			writeMemory(addr, B);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (ix+*),c
		case 0x71:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			writeMemory(addr, C);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (ix+*),d
		case 0x72:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			writeMemory(addr, D);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (ix+*),e
		case 0x73:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			writeMemory(addr, E);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (ix+*),h
		case 0x74:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			writeMemory(addr, H);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (ix+*),l
		case 0x75:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			writeMemory(addr, L);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (ix+*),a
		case 0x77:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			writeMemory(addr, A);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x7C:	//	ld a,ixh
			A = IX >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x7D:	//	ld a,ixl
			A = IX & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	ld a,(ix+*)
		case 0x7E:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			tmp = readMemory(addr);
			A = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x84:	//	add a,ixh
			A = cpuAdd(A, IX >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x85:	//	add a,ixl
			A = cpuAdd(A, IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	add a,(ix+*)
		case 0x86:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuAdd(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x8C:	//	adc a,ixh
			A = cpuAdc(A, IX >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x8D:	//	adc a,ixl
			A = cpuAdc(A, IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	adc a,(ix+*)
		case 0x8E:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuAdc(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x94:	//	sub ixh
			A = cpuSub(A, IX >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x95:	//	sub ixl
			A = cpuSub(A, IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	sub a,(ix+*)
		case 0x96:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuSub(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x9C:	//	sbc a,ixh
			A = cpuSubCarry(A, IX >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x9D:	//	sbc a,ixl
			A = cpuSubCarry(A, IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	sbc a,(ix+*)
		case 0x9E:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			A = cpuSubCarry(A, tmp);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xA4:	//	and ixh
			A = cpuAnd(A, IX >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0xA5:	//	and ixl
			A = cpuAnd(A, IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	and (ix+*)
		case 0xA6:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuAnd(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xAC:	//	xor ixh
			A = cpuXor(A, IX >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0xAD:	//	xor ixl
			A = cpuXor(A, IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	xor (ix+*)
		case 0xAE:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuXor(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xB4:	//	or ixh
			A = cpuOr(A, IX >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0xB5:	//	or ixl
			A = cpuOr(A, IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	or (ix+*)
		case 0xB6:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuOr(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xBC:	//	cp ixh
			cpuCmp(A, IX >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0xBD:	//	cp ixl
			cpuCmp(A, IX & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	cp (ix+*)
		case 0xBE:
			offset = readMemory(PC + 1);
			addr = (IX + offset) & 0xFFFF;
			data = readMemory(addr);
			
			cpuCmp(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xCB:	//	IX bit instructions (DDCB)
			cycles = extendedDDFDCB(IX);
			break;
		case 0xE1:	//	pop ix
			lo = readMemory(SP);
            hi = readMemory(SP + 1);
            tmp = (hi << 8) | lo;
            
            SP = (SP + 2) & 0xFFFF;
            IX = tmp;
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 14;
			break;
		//	ex (sp),ix
		case 0xE3:
			int tmpH = IX >> 8;
        	int tmpL = IX & 0xFF;
        	IX = (readMemory(SP + 1) << 8) | readMemory(SP);
        	
        	writeMemory(SP, tmpL);
        	writeMemory(SP + 1, tmpH);
        	
        	PC = (PC + 1) & 0xFFFF;
        	cycles = 23;
			break;
			
		//	PUSH IX		(SP – 2) <- IXL, (SP – 1) <- IXH
//		The contents of Index Register IX are pushed to the external memory last-in, first-out
//		(LIFO) stack. The Stack Pointer (SP) Register pair holds the 16-bit address of the current
//		top of the Stack. This instruction first decrements SP and loads the high-order byte of IX
//		to the memory address specified by SP; then decrements SP again and loads the low-order
//		byte to the memory location corresponding to this new address in SP
		case 0xE5:
			hi = IX >> 8;
            lo = IX & 0xFF;
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, hi);
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, lo);
			
            PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		//	jp (ix)
		case 0xE9:
			PC = IX;
			
			cycles = 8;
			break;
			
		//	ld sp,ix
		case 0xF9:
			SP = IX;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
			
		default:
			System.out.println("OJO QUE PUEDE FALTAR IMPLEMENTAR UNA INSTR ACA ! " + hex(extOpcode));
			cycles = executeInstruction(extOpcode);
			
			PC = (PC + 1) & 0xFFFF;
			
//			for (int i = 0; i < lastInstr.length; i++) {
//				System.out.println(lastInstr[i]);
//			}
//			throw new RuntimeException("Illegal opcode DD " + hex(extOpcode) + " found at pc: " + hex4(PC - 1));
		}
		
		return cycles;
	}
	
	private int extendedDDFDCB(int indexReg) {
		int tmp;
		int addr;
		int cycles = 0;
		byte sumando = (byte) readMemory(PC + 1);	// hasta 7F suma 127, de 80 a FF resta hasta 128
		addr = (indexReg + sumando) & 0xFFFF;
		int extOpcode = readMemory(PC + 2);
		switch (extOpcode) {
		case 0x06:
			tmp = cpuRlc(readMemory(addr));
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x0E:
			tmp = cpuRrc(readMemory(addr));
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x16:
			tmp = cpuRl(readMemory(addr));
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x1E:
			tmp = cpuRr(readMemory(addr));
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x26:
			tmp = cpuSla(readMemory(addr));
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x2E:
			tmp = cpuSra(readMemory(addr));
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x36:
			tmp = cpuSll(readMemory(addr));
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x3E:
			tmp = cpuSrl(readMemory(addr));
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
			
		case 0x46:
			tmp = readMemory(addr);
			cpuTestBit(tmp, 0);
			
			cycles = 20;
			break;
		case 0x4E:
			tmp = readMemory(addr);
			cpuTestBit(tmp, 1);
			
			cycles = 20;
			break;
		case 0x56:
			tmp = readMemory(addr);
			cpuTestBit(tmp, 2);
			
			cycles = 20;
			break;
		case 0x5E:
			tmp = readMemory(addr);
			cpuTestBit(tmp, 3);
			
			cycles = 20;
			break;
		case 0x66:
			tmp = readMemory(addr);
			cpuTestBit(tmp, 4);
			
			cycles = 20;
			break;
		case 0x6E:
			tmp = readMemory(addr);
			cpuTestBit(tmp, 5);
			
			cycles = 20;
			break;
		case 0x76:
			tmp = readMemory(addr);
			cpuTestBit(tmp, 6);
			
			cycles = 20;
			break;
		case 0x7E:
			tmp = readMemory(addr);
			cpuTestBit(tmp, 7);
			
			cycles = 20;
			break;
			
		case 0x86:
			tmp = readMemory(addr);
			tmp = bitReset(tmp, 0);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x8E:
			tmp = readMemory(addr);
			tmp = bitReset(tmp, 1);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x96:
			tmp = readMemory(addr);
			tmp = bitReset(tmp, 2);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0x9E:
			tmp = readMemory(addr);
			tmp = bitReset(tmp, 3);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xA6:
			tmp = readMemory(addr);
			tmp = bitReset(tmp, 4);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xAE:
			tmp = readMemory(addr);
			tmp = bitReset(tmp, 5);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xB6:
			tmp = readMemory(addr);
			tmp = bitReset(tmp, 6);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xBE:
			tmp = readMemory(addr);
			tmp = bitReset(tmp, 7);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
			
		case 0xC6:
			tmp = readMemory(addr);
			tmp = bitSet(tmp, 0);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xCE:
			tmp = readMemory(addr);
			tmp = bitSet(tmp, 1);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xD6:
			tmp = readMemory(addr);
			tmp = bitSet(tmp, 2);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xDE:
			tmp = readMemory(addr);
			tmp = bitSet(tmp, 3);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xE6:
			tmp = readMemory(addr);
			tmp = bitSet(tmp, 4);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xEE:
			tmp = readMemory(addr);
			tmp = bitSet(tmp, 5);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xF6:
			tmp = readMemory(addr);
			tmp = bitSet(tmp, 6);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
		case 0xFE:
			tmp = readMemory(addr);
			tmp = bitSet(tmp, 7);
			
			writeMemory(addr, tmp);
			cycles = 23;
			break;
			
			default:
				throw new RuntimeException("Illegal opcode DDCB " + hex(extOpcode) + " found at pc: " + hex4(PC - 1));
		}
		
		PC = (PC + 3) & 0xFFFF;
	
		R = (R + 1) & 0x7F;
		
		return cycles;
	}
	
	//	Same as xxIXxx, but instead with IY, IYH, and IYL.
	private int extendedFD() {
		int tmp, hi, lo, offset, data;
		int addr;
		int cycles = 0;
		int extOpcode = readMemory(PC);
		switch (extOpcode) {
		case 0x09:	//	add iy,bc
			tmp = add16Bits(IY, BC());
			IY = tmp;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		case 0x19:	//	add iy,de
			tmp = add16Bits(IY, DE());
			IY = tmp;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		case 0x21:	// LD IY, **
			lo = readMemory(PC + 1);
            hi = readMemory(PC + 2);
            tmp = (hi << 8) | lo;
			
            IY = tmp;
            
            PC = (PC + 3) & 0xFFFF;
			cycles = 14;
			break;
		//	ld (**),iy
		case 0x22:
			lo = readMemory(PC + 1);
            hi = readMemory(PC + 2);
            addr = (hi << 8) | lo;

            writeMemory(addr, (IY & 0xFF));
            writeMemory(addr + 1, (IY >> 8));
			
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			break;
		//	inc iy
		case 0x23:
			IY = (IY + 1) & 0xFFFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 10;
			break;
		//	inc iyh		unofficial
		case 0x24:
			tmp = (IY >> 8);
			tmp = cpuInc(tmp);
			IY = (tmp << 8) | (IY & 0x00FF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	dec iyh		unofficial
		case 0x25:
			tmp = (IY >> 8);
			tmp = cpuDec(tmp);
			IY = (tmp << 8) | (IY & 0x00FF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
			
		//	ld iyh,*	Unofficial
		case 0x26:
			data = readMemory(PC + 1);
            
			IY = (data << 8) | (IY & 0x00FF);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 11;
			break;
		//	add iy,iy
		case 0x29:
			tmp = add16Bits(IY, IY);
			IY = tmp;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		// ld iy,**
		case 0x2A:
			lo = readMemory(PC + 1);
            hi = readMemory(PC + 2);
            addr = (hi << 8) | lo;
            
            lo = readMemory(addr);
            hi = readMemory(addr + 1);
			
            IY = (hi << 8) | lo;
			
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			break;
		//	dec iy
		case 0x2B:
			IY = (IY - 1) & 0xFFFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 10;
			break;
			
		//	inc iyl
		case 0x2C:
			tmp = (IY & 0xFF);
			tmp = cpuInc(tmp);
			IY = (IY & 0xFF00) | (tmp);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	dec iyl
		case 0x2D:
			tmp = (IY & 0xFF);
			tmp = cpuDec(tmp);
			IY = (IY & 0xFF00) | (tmp);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		
		//	ld iyl,*	Unofficial
		case 0x2E:
			data = readMemory(PC + 1);
			
			IY = (IY & 0xFF00) | data;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 11;
			break;
			
		//	inc (iy+*)
		case 0x34:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			data = readMemory(addr);
			
			data = cpuInc(data);
			writeMemory(addr, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 23;
			break;
		//	dec (iy+*)
		case 0x35:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			data = readMemory(addr);
			
			data = cpuDec(data);
			writeMemory(addr, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 23;
			break;
		//	ld(iy+*),*
		case 0x36:
			offset = readMemory(PC + 1);
			data = readMemory(PC + 2);
			addr = (IY + offset) & 0xFFFF;
			
			writeMemory(addr, data);
			
			PC = (PC + 3) & 0xFFFF;
			cycles = 19;
			break;
			
		case 0x39:	//	add iy,sp
			tmp = add16Bits(IY, SP);
			IY = tmp;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		case 0x44:	//	ld b,iyh
			B = IY >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
        case 0x45:	//	ld b,iyl
			B = IY & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld b,(iy+*)
		case 0x46:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			B = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x4C:	//	ld c,iyh
			C = IY >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
        case 0x4D:	//	ld c,iyl
			C = IY & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld c,(iy+*)
		case 0x4E:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			C = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
        case 0x54:	//	ld d,iyh
			D = IY >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
        case 0x55:	//	ld d,iyl
			D = IY & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld d,(iy+*)
		case 0x56:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			D = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x5C:	//	ld e,iyh
			E = IY >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
        case 0x5D:	//	ld e,iyl
			E = IY & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld e,(iy+*)
		case 0x5E:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			E = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x60:	//	ld iyh,b
			IY = (B << 8) | (IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x61:	//	ld iyh,c
			IY = (C << 8) | (IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x62:	//	ld iyh,d
			IY = (D << 8) | (IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x63:	//	ld iyh,e
			IY = (E << 8) | (IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x64:	//	ld iyh,iyh		(no hace nada)
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x65:	//	ld iyh,iyl
			IY = ((IY & 0xFF) << 8) | (IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld h,(iy+*)
		case 0x66:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			H = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x67:	//	ld iyh,a
			IY = (A << 8) | (IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x68:	//	ld iyl,b
			IY = (IY & 0xFF00) | B;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x69:	//	ld iyl,c
			IY = (IY & 0xFF00) | C;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x6A:	//	ld iyl,d
			IY = (IY & 0xFF00) | D;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x6B:	//	ld iyl,e
			IY = (IY & 0xFF00) | E;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x6C:	//	ld iyl,iyh
			IY = (IY & 0xFF00) | ((IY & 0xFF00) >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x6D:	//	ld iyl,iyl		no hace nada
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		// ld l,(iy+*)
		case 0x6E:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			L = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x6F:	//	ld iyl,a
			IY = (IY & 0xFF00) | A;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	ld (iy+*),b
		case 0x70:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			writeMemory(addr, B);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (iy+*),c
		case 0x71:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			writeMemory(addr, C);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (iy+*),d
		case 0x72:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			writeMemory(addr, D);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (iy+*),e
		case 0x73:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			writeMemory(addr, E);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (iy+*),h
		case 0x74:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			writeMemory(addr, H);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (iy+*),l
		case 0x75:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			writeMemory(addr, L);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		//	ld (iy+*),a
		case 0x77:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			writeMemory(addr, A);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x7C:	//	ld a,iyh
			A = IY >> 8;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x7D:	//	ld a,iyl
			A = IY & 0xFF;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	ld a,(iy+*)
		case 0x7E:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			tmp = readMemory(addr);
			A = tmp;
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x84:	//	add a,iyh
			A = cpuAdd(A, IY >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x85:	//	add a,iyl
			A = cpuAdd(A, IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	add a,(iy+*)
		case 0x86:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuAdd(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x8C:	//	adc a,iyh
			A = cpuAdc(A, IY >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x8D:	//	adc a,iyl
			A = cpuAdc(A, IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	adc a,(iy+*)
		case 0x8E:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuAdc(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x94:	//	sub iyh
			A = cpuSub(A, IY >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x95:	//	sub iyl
			A = cpuSub(A, IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	sub a,(iy+*)
		case 0x96:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuSub(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0x9C:	//	sbc a,iyh
			A = cpuSubCarry(A, IY >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x9D:	//	sbc a,iyl
			A = cpuSubCarry(A, IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	sbc a,(iy+*)
		case 0x9E:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			tmp = readMemory(addr);
			
			A = cpuSubCarry(A, tmp);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xA4:	//	and iyh
			A = cpuAnd(A, IY >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0xA5:	//	and iyl
			A = cpuAnd(A, IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	and (iy+*)
		case 0xA6:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuAnd(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xAC:	//	xor iyh
			A = cpuXor(A, IY >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0xAD:	//	xor iyl
			A = cpuXor(A, IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	xor (iy+*)
		case 0xAE:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuXor(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xB4:	//	or iyh
			A = cpuOr(A, IY >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0xB5:	//	or iyl
			A = cpuOr(A, IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	or (iy+*)
		case 0xB6:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			data = readMemory(addr);
			
			A = cpuOr(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xBC:	//	cp iyh
			cpuCmp(A, IY >> 8);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0xBD:	//	cp iyl
			cpuCmp(A, IY & 0xFF);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		//	cp (iy+*)
		case 0xBE:
			offset = readMemory(PC + 1);
			addr = (IY + offset) & 0xFFFF;
			data = readMemory(addr);
			
			cpuCmp(A, data);
			
			PC = (PC + 2) & 0xFFFF;
			cycles = 19;
			break;
		case 0xCB:	//	IY bit instructions (DDCB)
			cycles = extendedDDFDCB(IY);
			break;
		case 0xE1:	//	pop iy
			lo = readMemory(SP);
            hi = readMemory(SP + 1);
            tmp = (hi << 8) | lo;
            
            SP = (SP + 2) & 0xFFFF;
            IY = tmp;
            
            PC = (PC + 1) & 0xFFFF;
            cycles = 14;
			break;
		//	ex (sp),iy
		case 0xE3:
			int tmpH = IY >> 8;
        	int tmpL = IY & 0xFF;
        	IY = (readMemory(SP + 1) << 8) | readMemory(SP);
        	
        	writeMemory(SP, tmpL);
        	writeMemory(SP + 1, tmpH);
        	
        	PC = (PC + 1) & 0xFFFF;
        	cycles = 23;
			break;
			
		//	PUSH IY		(SP – 2) <- IYL, (SP – 1) <- IYH
		case 0xE5:
			hi = IY >> 8;
            lo = IY & 0xFF;
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, hi);
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, lo);
			
            PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		//	jp (iy)
		case 0xE9:
			PC = IY;
			
			cycles = 8;
			break;
			
		//	ld sp,iy
		case 0xF9:
			SP = IY;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
			
		default:
			System.out.println("OJO QUE PUEDE FALTAR IMPLEMENTAR UNA INSTR ACA !" + hex(extOpcode));
			cycles = executeInstruction(extOpcode);
			
			PC = (PC + 1) & 0xFFFF;
			
//			throw new RuntimeException("Illegal opcode FD " + hex(extOpcode) + " found at pc: " + hex4(PC - 1));
		}
		
		return cycles;
	}
	
	private int extendedED(int opcode) {
		int tmp;
		int addr;
		int extOpcode;
		int hi, lo;
		int cycles = 0;
		extOpcode = readMemory(PC);
		switch (extOpcode) {
		
		//	in b,(c)
		case 0x40:
			B = cpuInPort(C);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	out (c),b
		case 0x41:
			bus.outPort(C, B);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	sbc hl,bc
		case 0x42:
			tmp = cpuSbc16Bits(HL(), BC());
	        HL(tmp);
	        PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		//	ld (**),bc
		case 0x43:
			lo = readMemory(PC + 1);
			hi = readMemory(PC + 2);
			addr = (hi << 8) | lo; 
			
			writeMemory(addr, C);
			writeMemory(addr + 1, B);
			
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			break;
		// NEG		This command literally subtracts A from 0
		case 0x44:
            clearCarryFlag();
            A = cpuSubCarry(0, A);
			
//			A = cpuSub(0, A);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		
		case 0x45:		//	retn
		case 0x4D:		//	RETI	TODO hay una diferencia entre retn y reti, http://www.z80.info/z80_faq.htm#Q-13
            lo = readMemory(SP);
            hi = readMemory(SP + 1);
            tmp = (hi << 8) | lo;
      
            FF1 = FF2;
            
            SP = (SP + 2) & 0xFFFF;
            PC = tmp;
            
			cycles = 14;
			break;
			
		//	LD I,A
		case 0x47:
			I = A;	// TODO agregarle cycles
			
			PC = (PC + 1) & 0xFFFF;
			break;
			
		//	in c,(c)
		case 0x48:
			C = cpuInPort(C);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	out (c),c
		case 0x49:
			bus.outPort(C, C);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	adc hl,bc
		case 0x4A:
			tmp = cpuAdc16Bits(HL(), BC());
			HL(tmp);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		// LD bc,(nn)
		case 0x4B:
			lo = readMemory(PC + 1);
            hi = readMemory(PC + 2);
            addr = (hi << 8) | lo;
			
            C = readMemory(addr);
            B = readMemory(addr + 1);
            
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			break;
			
		//	LD R,A
		case 0x4F:
			R = A;	// TODO agregarle cycles
			
			PC = (PC + 1) & 0xFFFF;
			break;
			
		//	in d,(c)
		case 0x50:
			D = cpuInPort(C);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	out (c), d
		case 0x51:
			bus.outPort(C, D);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	sbc hl,de
		case 0x52:
			tmp = cpuSbc16Bits(HL(), DE());
			HL(tmp);
	        PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		//	ld (**),de
		case 0x53:
			lo = readMemory(PC + 1);
			hi = readMemory(PC + 2);
			addr = (hi << 8) | lo; 
			
			writeMemory(addr, E);
			writeMemory(addr + 1, D);
			
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			
			break;
		case 0x56:	//	IM 1
			interruptMode = 1;

			PC = (PC + 1) & 0xFFFF;
			cycles = 8;
			break;
		case 0x57:
			A = I;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 9;
			break;
		//	in e,(c)
		case 0x58:
			E = cpuInPort(C);

			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	out (c),e
		case 0x59:
			bus.outPort(C, E);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	adc hl,de
		case 0x5A:
			tmp = cpuAdc16Bits(HL(), DE());
			HL(tmp);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		// LD DE,(nn)
		case 0x5B:
			lo = readMemory(PC + 1);
            hi = readMemory(PC + 2);
            addr = (hi << 8) | lo;
			
            E = readMemory(addr);
            D = readMemory(addr + 1);
            
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			break;
		// ld a,r
		case 0x5F:
			A = R;
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 9;
			break;
		//	in h,(c)
		case 0x60:
			H = cpuInPort(C);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	out (c),h
		case 0x61:
			bus.outPort(C, H);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	sbc hl,hl
		case 0x62:
			tmp = cpuSbc16Bits(HL(), HL());
			HL(tmp);
	        PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		//	rrd
		case 0x67:
			tmp = readMemory(HL());
			int nuevo = ((A & 0xF) << 4) | tmp >> 4;
			
			writeMemory(HL(), nuevo);
			
            A = (A & 0xF0) | (tmp & 0xF);
            
            calculateSignZero(A);
            
			clearNegativeFlag();
			clearHalfCarryFlag();
			calculateParity(A);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 18;
			break;
		//	in l,(c)
		case 0x68:
			L = cpuInPort(C);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	out (c),l
		case 0x69:
			bus.outPort(C, L);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	adc hl,hl
		case 0x6A:
			tmp = cpuAdc16Bits(HL(), HL());
			HL(tmp);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		//	rld
		case 0x6F:
			tmp = readMemory(HL());
			
			int tmpA = A;
			A = (A & 0xF0) | ((tmp & 0xF0) >> 4);
			int data = ((tmp & 0x0F) << 4) | (tmpA & 0x0F);
			
			writeMemory(HL(), data);
			
			calculateSignZero(A);
			
			clearNegativeFlag();
			clearHalfCarryFlag();
			calculateParity(A);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 18;
			break;
			
		//	sbc hl,sp
		case 0x72:
			tmp = cpuSbc16Bits(HL(), SP);
			HL(tmp);
	        PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		//	ld (**),sp
		case 0x73:
			lo = readMemory(PC + 1);
			hi = readMemory(PC + 2);
			addr = (hi << 8) | lo; 
			
			writeMemory(addr, (SP & 0xFF));
			writeMemory(addr + 1, (SP >> 8));
			
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			break;
		//	in a,(c)
		case 0x78:
			A = cpuInPort(C);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	out (c),a
		case 0x79:
			bus.outPort(C, A);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 12;
			break;
		//	adc hl,sp
		case 0x7A:
			tmp = cpuAdc16Bits(HL(), SP);
			HL(tmp);
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 15;
			break;
		// LD sp,(nn)
		case 0x7B:
			lo = readMemory(PC + 1);
            hi = readMemory(PC + 2);
            addr = (hi << 8) | lo;
			
            lo = readMemory(addr);
            hi = readMemory(addr + 1);
            SP = (hi << 8) | lo;
            
			PC = (PC + 3) & 0xFFFF;
			cycles = 20;
			break;
		// ldi
		case 0xA0:
			tmp = readMemory(HL());
			writeMemory(DE(), tmp);
			
			HL((HL() + 1) & 0xFFFF);
			DE((DE() + 1) & 0xFFFF);
			BC((BC() - 1) & 0xFFFF);
			
			if (BC() != 0) {
				setOverflowFlag();
			} else {
				clearOverflowFlag();
			}
			
			clearNegativeFlag();
			clearHalfCarryFlag();
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 16;
			
			break;
			
		//	cpi
		// The carry is preserved, N is set and all the other flags are affected
		// as defined. P/V denotes the overflowing of BC, while the Z flag is
		// set if A=(HL) before HL is increased.
//			PV set if BC not 0
//          S,Z,H from (A - (HL) ) as in CP (HL)
		case 0xA1:
			tmp = readMemory(HL());
			boolean preserveCarry = isCarryFlagSet();	// hack porque esta instruccion no modifica el carry, pero el cmp comun si
			
			cpuCmp(A, tmp);
			
			if (preserveCarry) {
				setCarryFlag();
			} else {
				clearCarryFlag();
			}
			
			HL((HL() + 1) & 0xFFFF);
			BC((BC() - 1) & 0xFFFF);
			
			if (BC() != 0) {
				setOverflowFlag();
			} else {
				clearOverflowFlag();
			}
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 16;
			
			break;
			
		//	ini
		// C is preserved, the N flag is reset. S, H and P/V are undefined. Z is
		// set if B becomes zero after decrementing, otherwise it is reset.
		case 0xA2:
			tmp = cpuInPort(C);
			writeMemory(HL(), tmp);
			
			HL((HL() + 1) & 0xFFFF);
			B = (B - 1) & 0xFF;
			
			if (B == 0) {
				setZeroFlag();
			} else {
				clearZeroFlag();
			}
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 16;
			break;
		// OUTI		(C) = (HL), B = B – 1, HL = HL + 1
		//	The contents of the HL register pair are placed on the address bus to select a location in
//        		memory. The byte contained in this memory location is temporarily stored in the CPU.
//        		Then, after the byte counter (B) is decremented, the contents of Register C are placed on
//        		the bottom half (A0 through A7) of the address bus to select the I/O device at one of 256
//        		possible ports. Register B can be used as a byte counter, and its decremented value is
//        		placed on the top half (A8 through A15) of the address bus. The byte to be output is placed
//        		on the data bus and written to a selected peripheral device. Finally, the register pair HL is
//        		incremented.
			//	Z is set if B – 1 = 0; otherwise, it is reset.
			//	N is set.
		case 0xA3:
			tmp = readMemory(HL());
			
			B = (B - 1) & 0xFF;
			lo = C;
			hi = B;	// para que esto ?
			
			bus.outPort(C, tmp);
			
			HL((HL() + 1) & 0xFFFF);

			if (B == 0) {
				setZeroFlag();
			} else {
				clearZeroFlag();
			}
			
			setNegativeFlag();
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 16;
			break;
		
		//	ldd
		case 0xA8:
			tmp = readMemory(HL());
			writeMemory(DE(), tmp);
			
			HL((HL() - 1) & 0xFFFF);
			DE((DE() - 1) & 0xFFFF);
			BC((BC() - 1) & 0xFFFF);
			
			if (BC() != 0) {
				setOverflowFlag();
			} else {
				clearOverflowFlag();
			}
			
			clearNegativeFlag();
			clearHalfCarryFlag();
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 16;
			
			break;
			
		//	cpd
		case 0xA9:
			int regHL = HL();
			int memHL = readMemory(HL());
	        preserveCarry = isCarryFlagSet(); // lo guardo porque cp lo toca
	        cpuCmp(A, memHL);
	        
	        if (preserveCarry) {
	        	setCarryFlag();
	        } else {
	        	clearCarryFlag();
	        }
	        
	        HL((HL() - 1) & 0xFFFF);
			BC((BC() - 1) & 0xFFFF);
	        
	        if (BC() != 0) {
	            setOverflowFlag();
	        } else {
	        	clearOverflowFlag();
	        }
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 16;
			
			break;
			
		//	outd	C is preserved, N is set, H, S, and P/V are undefined. Z is set only if B becomes zero after decrement, otherwise it is reset.
		case 0xAB:
			tmp = readMemory(HL());
			bus.outPort(C, tmp);
			
			HL((HL() - 1) & 0xFFFF);
			B = (B - 1) & 0xFF;
			
			setNegativeFlag();
			if (B == 0) {
				setZeroFlag();
			} else {
				clearZeroFlag();
			}
			
			PC = (PC + 1) & 0xFFFF;
			cycles = 16;
			break;
		// ldir
		//	Repeats LDI (LD (DE),(HL), then increments DE, HL, and decrements BC) until BC=0.
		//	Note that if BC=0 before this instruction is called, it will loop around until BC=0 again.
		//	interrupts can trigger
		//	P/V is reset.
		case 0xB0:
			tmp = readMemory(HL());
			writeMemory(DE(), tmp);
			
			DE((DE() + 1) & 0xFFFF);
			HL((HL() + 1) & 0xFFFF);
			BC((BC() - 1) & 0xFFFF);
				
			clearParityFlag();
			clearNegativeFlag();
			clearHalfCarryFlag();
			
			if (BC() != 0) {
				PC = (PC - 1) & 0xFFFF;
				cycles = 21;
			} else {
				PC = (PC + 1) & 0xFFFF;
				cycles = 16;
			}
			
			break;
			
		//	cpir
//			Repeats CPI until either:
//				BC=0
//				A=HL
		//	P/V denotes the overflowing of BC, while the Z flag is set if A=(HL) before HL is decreased.
		case 0xB1:
			tmp = readMemory(HL());
			preserveCarry = isCarryFlagSet();	// hack porque esta instruccion no modifica el carry, pero el cmp comun si
			
			cpuCmp(A, tmp);
			
			if (preserveCarry) {
				setCarryFlag();
			} else {
				clearCarryFlag();
			}
			
			HL((HL() + 1) & 0xFFFF);
			BC((BC() - 1) & 0xFFFF);
			
			if (BC() != 0) {
				setOverflowFlag();
			} else {
				clearOverflowFlag();
			}
			
			if (BC() != 0 && A != tmp) {
				PC = (PC - 1) & 0xFFFF;
				cycles = 21;
			} else {
				PC = (PC + 1) & 0xFFFF;
				cycles = 16;
			}
			
			break;
			
		//	Reads from (HL) and writes to the (C) port. HL is incremented and B is decremented. Repeats until B = 0.
		//	Z is set, C is preserved, N is reset, H, S, and P/V are undefined.
		//	B=0 -> 16 cycles   B != 0 -> 21 cycles
		case 0xB3:		// OTIR
			tmp = readMemory(HL());
			bus.outPort(C, tmp);
			HL(HL() + 1 & 0xFFFF);
			B = (B - 1) & 0xFF;
			
			if (B == 0) {
				setZeroFlag();
			} else {
				clearZeroFlag();
			}
			setNegativeFlag();		// TODO incluir todos estos flags en el calculo
			setHalfCarryFlag();			//	en emulador MEKA se setean estas al final parece
			setCarryFlag();
			clearSignFlag();
			
			if (B != 0) {
				PC = (PC - 1) & 0xFFFF;
				cycles = 21;
			} else {
				PC = (PC + 1) & 0xFFFF;
				cycles = 16;
			}
			
			break;
		// LDDR
		//	This 2-byte instruction transfers a byte of data from the memory location addressed by the
//        		contents of the HL register pair to the memory location addressed by the contents of the
//        		DE register pair. Then both of these registers, and the BC (Byte Counter), are decremented.
//        		If decrementing causes BC to go to 0, the instruction is terminated. If BC is not 0,
//        		the program counter is decremented by two and the instruction is repeated. Interrupts are
//        		recognized and two refresh cycles execute after each data transfer.
			//	H is reset.
			//	P/V is reset
		case 0xB8:
			tmp = readMemory(HL());
			writeMemory(DE(), tmp);
			
			HL((HL() - 1) & 0xFFFF);
			DE((DE() - 1) & 0xFFFF);
			BC((BC() - 1) & 0xFFFF);
			
			clearParityFlag();
			clearNegativeFlag();
			clearHalfCarryFlag();
			
			if (BC() != 0) {
				PC = (PC - 1) & 0xFFFF;
				cycles = 21;
			} else {
				PC = (PC + 1) & 0xFFFF;
				cycles = 16;
			}
			
			break;
		//	cpdr
		case 0xB9:
			tmp = readMemory(HL());
			preserveCarry = isCarryFlagSet();	// hack porque esta instruccion no modifica el carry, pero el cmp comun si
			
			// TODO verificar todas estas instrucciones raras, el Golden axe se cuelga seguramente por esto
			
			cpuCmp(A, tmp);
			
			if (preserveCarry) {
				setCarryFlag();
			} else {
				clearCarryFlag();
			}
//			if (isZeroFlagSet()) {		// El MEKA las tiene invertidas ? no claro porque TODO
//				clearZeroFlag();
//			} else {
//				setZeroFlag();
//			}
			
			HL((HL() - 1) & 0xFFFF);
			BC((BC() - 1) & 0xFFFF);
			
			//	P/V denotes the overflowing of BC
			if (BC() != 0) {
				setOverflowFlag();
			} else {
				clearOverflowFlag();
			}
			
			setNegativeFlag();
			
			if (BC() != 0 && A != tmp) {
				PC = (PC - 1) & 0xFFFF;
				cycles = 21;
			} else {
				PC = (PC + 1) & 0xFFFF;
				cycles = 16;
			}
			
			break;
			
		default:
			for (int i = 0; i < lastInstr.length; i++) {
				System.out.println(lastInstr[i]);
			}
			throw new RuntimeException("Illegal opcode " + hex(opcode) + hex(extOpcode) + " found at pc: " + hex4(PC - 1));
		}
		return cycles;
	}

	private void calculateSignZero(int reg) {
		F = (F & 0x3F) | (flagsSZ[reg] & 0xC0);
	}

	private int cpuInPort(int port) {
		int tmp = bus.inPort(port);

		calculateSignZero(tmp);
		
		clearHalfCarryFlag();
		clearNegativeFlag();
		calculateParity(tmp);
		
		return tmp;
	}

	private int cpuSbc16Bits(int a, int b) {
		int tmp = a - b;
		if (isCarryFlagSet()) {
			tmp--;
		}

		if (tmp < 0) {
		    setCarryFlag();
		} else {
		    clearCarryFlag();
		}
		
		tmp = tmp & 0xFFFF;
        
        if (tmp == 0) {
        	setZeroFlag();
        } else {
        	clearZeroFlag();
        }
        if ((tmp & 0x8000) > 0) {
        	setSignFlag();
        } else {
        	clearSignFlag();
        }
        if (((tmp ^ a ^ b) & 0x1000) != 0) {
            setHalfCarryFlag();
        } else {
        	clearHalfCarryFlag();
        }
        if (((a ^ b) & (a ^ tmp)) > 0x7fff) {
            setOverflowFlag();
        } else {
        	clearOverflowFlag();
        }
        
        setNegativeFlag();
        
        return tmp;
	}

	private void cpuCmp(int reg, int toCompare) {
        int tmp = reg - toCompare;
        
        if (tmp < 0) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        
        tmp &= 0xff;
        
        if ((tmp & 0xF) > (reg & 0xF)) {
            setHalfCarryFlag();
        } else {
            clearHalfCarryFlag();
        }
        
        calculateSignZero(tmp);
        
        if (((reg ^ toCompare) & (reg ^ tmp)) > 0x7f) {
            setOverflowFlag();
        } else {
        	clearOverflowFlag();
        }
        
        setNegativeFlag();
    }

    private int cpuSubCarry(int reg, int subs) {
        int tmp = reg - subs - (isCarryFlagSet() ? 1 : 0);
        if (tmp < 0) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        
        tmp = tmp & 0xFF;
        
        if (((reg ^ subs ^ tmp) & 0x10) != 0) {
        	setHalfCarryFlag();
        } else {
        	clearHalfCarryFlag();
        }
        
        calculateSignZero(tmp);
        
        if (((reg ^ subs) & (reg ^ tmp)) > 0x7f) {
        	setOverflowFlag();
        } else {
        	clearOverflowFlag();
        }
        
        setNegativeFlag();
        
        return tmp;
    }

    private int cpuAdc(int reg, int toAdd) {
        int tmp = reg + toAdd + (isCarryFlagSet() ? 1 : 0);

        if (tmp > 0xFF) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        
        int result = tmp & 0xFF;
        
        calculateSignZero(result);
        
        if (((reg ^ toAdd ^ result) & 0x10) != 0) {
            setHalfCarryFlag();
        } else {
        	clearHalfCarryFlag();
        }
        if (((reg ^ ~toAdd) & (reg ^ result)) > 0x7f) {
        	setOverflowFlag();
        } else {
        	clearOverflowFlag();
        }
        
        clearNegativeFlag();
        
        return result;
    }

//    The overflow assumes signed operation. That is, it is set if a value crosses over the maximum possible integer (127, or 0x7f), or below the minimum possible integer (-128, or 0x80). 
//    As for how it affects DEC/INC, V is set on inc if the result is 0x80, and V is set on dec if the result is 0x7f. Otherwise, it's cleared. 
    private int cpuInc(int reg) {
        reg = (reg + 1) & 0xFF;
        
        if ((reg & 0xF) == 0) {
            setHalfCarryFlag();
        } else {
            clearHalfCarryFlag();
        }
        if (reg == 0x80) {
        	setOverflowFlag();
        } else {
        	clearOverflowFlag();
        }
        
        calculateSignZero(reg);
        clearNegativeFlag();
        
        return reg;
    }

//    S is not affected.
//    Z is not affected.
//    H is set if carry from bit 11; otherwise, it is reset.
//    P/V is not affected.
//    N is reset.
//	  C is set if carry from bit 15; otherwise, it is reset.
    private int add16Bits(int reg, int toAdd) {
        int tmp = reg + toAdd;
        if ((tmp & 0xFFF) < (reg & 0xFFF)) {
            setHalfCarryFlag();
        } else {
            clearHalfCarryFlag();
        }
        if (tmp > 0xFFFF) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        clearNegativeFlag();
        
        return tmp & 0xFFFF;
    }
    
    int cpuAdc16Bits(int a, int b) {
    	int tmp = a + b;
    	if (isCarryFlagSet()) {
    		tmp++;
    	}
    	
        if (tmp > 0xFFFF) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        
        tmp = tmp & 0xFFFF;
        
        if (tmp == 0) {
        	setZeroFlag();
        } else {
        	clearZeroFlag();
        }
        if ((tmp & 0x8000) > 0) {
        	setSignFlag();
        } else {
        	clearSignFlag();
        }
        if (((tmp ^ a ^ b) & 0x1000) != 0) {
            setHalfCarryFlag();
        } else {
        	clearHalfCarryFlag();
        }
        if (((a ^ ~b) & (a ^ tmp)) > 0x7fff) {
            setOverflowFlag();
        } else {
        	clearOverflowFlag();
        }
        
        clearNegativeFlag();
        
        return tmp;
    }
    
    private int cpuDec(int reg) {
        reg = (reg - 1) & 0xFF;
        
        if ((reg & 0xF) == 0xF) {
            setHalfCarryFlag();
        } else {
            clearHalfCarryFlag();
        }
        if (reg == 0x7F) {
        	setOverflowFlag();
        } else {
        	clearOverflowFlag();
        }
        calculateSignZero(reg);
        setNegativeFlag();
        
        return reg;
    }

    private int cpuSub(int reg, int subs) {
        int tmp = reg - subs;
        if (tmp < 0) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        
        tmp = tmp & 0xFF;
        
        if (((reg ^ subs ^ tmp) & 0x10) != 0) {
        	setHalfCarryFlag();
        } else {
        	clearHalfCarryFlag();
        }
        
        calculateSignZero(tmp);
        
        if (((reg ^ subs) & (reg ^ tmp)) > 0x7f) {
        	setOverflowFlag();
        } else {
        	clearOverflowFlag();
        }
        
        setNegativeFlag();
        
        return tmp;
    }
    
    private int cpuAdd(int reg, int toAdd) {
        int tmp = reg + toAdd;
        if (tmp > 0xFF) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        
        int result = tmp & 0xFF;
        
        if (((reg ^ toAdd ^ result) & 0x10) != 0) {
            setHalfCarryFlag();
        } else {
        	clearHalfCarryFlag();
        }
        if (((reg ^ ~toAdd) & (reg ^ result)) > 0x7f) {
        	setOverflowFlag();
        } else {
        	clearOverflowFlag();
        }
        
        calculateSignZero(result);
        clearNegativeFlag();
        
        return result;
    }
    
    private int cpuRl(int reg) {
        boolean newFCarry = (reg > 0x7F);
        reg = ((reg << 1) & 0xFF) | ((isCarryFlagSet()) ? 1 : 0);
        if (newFCarry) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        
        calculateSignZero(reg);
        clearHalfCarryFlag();
        clearNegativeFlag();
        calculateParity(reg);

        return reg;
    }

    private int cpuXor(int reg, int toXor) {
        reg ^= toXor;

        calculateSignZero(reg);
        
        clearCarryFlag();
        clearHalfCarryFlag();
        clearNegativeFlag();
        calculateParity(reg);

        return reg;
    }
    
    private int cpuOr(int reg, int toOr) {
        reg = reg | toOr;
        
        calculateSignZero(reg);
        
        clearCarryFlag();
        clearHalfCarryFlag();
        clearNegativeFlag();
        calculateParity(reg);
        
        return reg;
    }

    private int cpuAnd(int reg, int toAnd) {
        reg = reg & toAnd;
        
        calculateSignZero(reg);
        
        clearCarryFlag();
        setHalfCarryFlag();
        clearNegativeFlag();
        calculateParity(reg);
        
        return reg;
    }

    private int jump(boolean condition) {
        byte n = (byte) readMemory(PC);

        int cycles = 0;
        
        if (condition) {
            PC += n;
            
            cycles = 12;
        } else {
        	cycles = 7;
        }

        PC = (PC + 1) & 0xFFFF;
    
        return cycles;
    }

    private int call(boolean condition) {
    	int cycles = 0;

    	int lo = readMemory(PC);
        int hi = readMemory(PC + 1);

        int nn = hi << 8 | lo;
        PC = (PC + 2) & 0xFFFF;
        
        if (condition) {
            hi = PC >> 8;
            lo = PC & 0xFF;
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, hi);
            
            SP = (SP - 1) & 0xFFFF;
            writeMemory(SP, lo);
            
            PC = nn;
            
            cycles = 17;
        } else {
        	cycles = 10;
        }
        
        return cycles;
    }

    private int cpuReturn(boolean condition) {
        int cycles = 0;
    	
    	if (condition) {
        	int word = readMemory(SP);
            word |= (readMemory(SP + 1) << 8);
            
            SP = (SP + 2) & 0xFFFF;
            PC = word;
            
            cycles = 11;
        } else {
        	cycles = 5;
        }
        
        return cycles;
    }

    private int cpuRr(int reg) {
        boolean isCarrySet = isCarryFlagSet();
        boolean isLSBSet = bitTest(reg, 0);

        reg = reg >> 1;

        if (isCarrySet) {
        	reg = bitSet(reg, 7);
        }
        
        if (isLSBSet) {
        	setCarryFlag();
        } else {
        	clearCarryFlag();
        }
        calculateSignZero(reg);
        
        clearNegativeFlag();
        clearHalfCarryFlag();
        calculateParity(reg);

        return reg;
    }
    
    private int cpuRrc(int reg) {
        if ((reg & 0x01) == 0x01) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        reg = ((isCarryFlagSet()) ? 0x80 : 0) | (reg >> 1);
        
        calculateSignZero(reg);
        
        clearHalfCarryFlag();
        clearNegativeFlag();
        calculateParity(reg);
        
        return reg;
    }

//    SF flag Set if n = 7 and tested bit is set.
//    ZF flag Set if the tested bit is reset.
//    YF flag Set if n = 5 and tested bit is set.
//    HF flag Always set.
//    XF flag Set if n = 3 and tested bit is set.
//    PF flag Set just like ZF flag.
//    NF flag Always reset.
//    CF flag Unchanged.
    void cpuTestBit(int reg, int bit) {
        
    	clearSignFlag();
    	
    	if (bitTest(reg, bit)) {
        	clearZeroFlag();
        	clearParityFlag();
        	if (bit == 7) {
        		setSignFlag();		// solo set si el bit a probar es 7 y esta set
        	}
        } else {
        	setZeroFlag();
        	setParityFlag();
        }

        clearNegativeFlag();
        setHalfCarryFlag();
    }

    private int cpuSrl(int reg) {
        if (bitTest(reg, 0)) {
            setCarryFlag();
        } else {
        	clearCarryFlag();
        }
        reg = reg >> 1;
        
        calculateSignZero(reg);
        
        clearNegativeFlag();
        clearHalfCarryFlag();
        calculateParity(reg);
        
        return reg;
    }
    
    private int cpuSra(int reg) {
        boolean isLSBSet = bitTest(reg, 0);
        boolean isMSBSet = bitTest(reg, 7);

        reg = reg >> 1;

        if (isMSBSet) {
        	reg = bitSet(reg, 7);
        }
    
        if (isLSBSet) {
        	setCarryFlag();
        } else {
        	clearCarryFlag();
        }
        calculateSignZero(reg);
        
        clearHalfCarryFlag();
        clearNegativeFlag();
        calculateParity(reg);

        return reg;
    }

    private int cpuSla(int reg) {
        boolean isMSBSet = bitTest(reg, 7);

        reg = (reg << 1) & 0xFF;

        if (isMSBSet) {
        	setCarryFlag();
        } else {
        	clearCarryFlag();
        }
        calculateSignZero(reg);

        clearNegativeFlag();
        clearHalfCarryFlag();
        calculateParity(reg);
        
        return reg;
    }
    
    //	unofficial
    //	Functions like sla, except a 1 is inserted into the low bit.
    private int cpuSll(int reg) {
        boolean isMSBSet = bitTest(reg, 7);

        reg = (reg << 1) & 0xFF;
        reg = reg | 0x1;
        
        if (isMSBSet) {
        	setCarryFlag();
        } else {
        	clearCarryFlag();
        }
        calculateSignZero(reg);

        clearNegativeFlag();
        clearHalfCarryFlag();
        calculateParity(reg);
        
        return reg;
    }

    private int cpuRlc(int reg) {
        if ((reg & 0x80) > 0) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        reg = ((reg << 1) & 0xFF) | ((isCarryFlagSet()) ? 1 : 0);
        
        calculateSignZero(reg);
        
        clearNegativeFlag();
        clearHalfCarryFlag();
        calculateParity(reg);
        
        return reg;
    }
    
    private void cpuDaa() {
    	int suma = 0;
        boolean carry = isCarryFlagSet();

        if ((isHalfCarryFlagSet() || (A & 0x0F) > 0x09)) {
            suma = 6;
        }

        if (carry || (A > 0x99)) {
            suma |= 0x60;
        }

        if (A > 0x99) {
            carry = true;
        }

        clearCarryFlag();
        if ((isNegativeFlagSet())) {
            A = cpuSubCarry(A, suma);
        } else {
            A = cpuAdc(A, suma);
        }
        
        calculateSignZero(A);
        calculateParity(A);

        if (carry) {
        	setCarryFlag();
        } else {
        	clearCarryFlag();
        }
    }
    
    void cpuRestart(int address) {
        int hi = PC >> 8;
        int lo = PC & 0xFF;

        SP = (SP - 1) & 0xFFFF;
        writeMemory(SP, hi);
        
        SP = (SP - 1) & 0xFFFF;
        writeMemory(SP, lo);
        
        PC = address;
    }
	
    private void ccf() {
        clearNegativeFlag();
        if (isCarryFlagSet()) {
            clearCarryFlag();
            setHalfCarryFlag();
        } else {
            setCarryFlag();
            clearHalfCarryFlag();
        }
    }

    private void sfc() {
        clearNegativeFlag();
        clearHalfCarryFlag();
        setCarryFlag();
    }

    private void cpl() {
        A = A ^ 0xFF;
        setNegativeFlag();
        setHalfCarryFlag();
    }

//    The contents of the Accumulator (Register A) are rotated right 1 bit position through the Carry flag.
//    The previous contents of the Carry flag are copied to bit 7. Bit 0 is the least significant bit.
//    S is not affected.
//    Z is not affected.
//    H is reset.
//    P/V is not affected.
//    N is reset.
//    C is data from bit 0 of Accumulator.
    private void rra() {
        boolean carryFlag = isCarryFlagSet();
        if ((A & 1) == 1) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        A = (A >> 1);
        if (carryFlag) {
        	A = A | 0x80;
        }
        
        clearHalfCarryFlag();
        clearNegativeFlag();
    }

//    The contents of the Accumulator (Register A) are rotated left 1 bit position through the Carry flag.
//    The previous contents of the Carry flag are copied to bit 0. Bit 0 is the least significant bit
//    S is not affected.
//    Z is not affected.
//    H is reset.
//    P/V is not affected.
//    N is reset.
//    C is data from bit 7 of Accumulator
    private void rla() {
        int carryFlag = (isCarryFlagSet()) ? 1 : 0;
        if ((A & 0x80) > 0) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        A = ((A << 1) & 0xFF) | carryFlag;
       
        clearHalfCarryFlag();
        clearNegativeFlag();
    }

//    The contents of the Accumulator (Register A) are rotated right 1 bit position. Bit 0 is copied
//    to the Carry flag and also to bit 7. Bit 0 is the least-significant bit.
//    S is not affected.
//    Z is not affected.
//    H is reset.
//    P/V is not affected.
//    N is reset.
//    C is data from bit 0 of Accumulator.
    private void rrca() {
    	boolean oldBit0 = bitTest(A, 0);
        A = (A >> 1);
        if (oldBit0) {
            setCarryFlag();
            A = A | 0x80;
        } else {
            clearCarryFlag();
        }
        
        clearHalfCarryFlag();
        clearNegativeFlag();
    }

//    The contents of the Accumulator (Register A) are rotated left 1 bit position. The sign bit
//    (bit 7) is copied to the Carry flag and also to bit 0. Bit 0 is the least-significant bit.
//    S is not affected.
//    Z is not affected.
//    H is reset.
//    P/V is not affected.
//    N is reset.
//    C is data from bit 7 of Accumulator.
    private void rlca() {
    	boolean bit7Set = bitTest(A, 7);
        if (bit7Set) {
            setCarryFlag();
        } else {
            clearCarryFlag();
        }
        A = ((A << 1) & 0xFF);
        if (isCarryFlagSet()) {
        	A = A | 1;
        }
        
        clearHalfCarryFlag();
        clearNegativeFlag();
    }
    
    int YMA0;
    int YMD0;
    int YMA1;
    int YMD1;
    int romBank68k;
    
    void writeMemory(int address, int data) {
    	if (address < 0x2000) {
    		memory[address] = data;
		} else if (address >= 0x2000 && address < 0x3FFF) {
			//	RESERVED
		} else if (address == 0x4000) {		//	YM2612 A0
			YMA0 = data;
		} else if (address == 0x4001) {		//	YM2612 D0
			YMD0 = data;
		} else if (address == 0x4002) {		//	YM2612 A1
			YMA1 = data;
		} else if (address == 0x4003) {		//	YM2612 D1
			YMD1 = data;
		} else if (address == 0x6000) {		//	rom banking
			romBank68k = data;
		} else {
			throw new RuntimeException("NOT " + Integer.toHexString(address));
		}
	}

	int readMemory(int address) {
		if (address < 0x2000) {
			return memory[address];
		} else if (address >= 0x2000 && address < 0x3FFF) {
			return 0;
		} else if (address == 0x4000) {		//	YM2612 A0
			return 0;	// TODO implement
		} else if (address == 0x4001) {		//	YM2612 D0
			throw new RuntimeException("NOT " + Integer.toHexString(address));
		} else if (address == 0x4002) {		//	YM2612 A1
			throw new RuntimeException("NOT " + Integer.toHexString(address));
		} else if (address == 0x4003) {		//	YM2612 D1
			throw new RuntimeException("NOT " + Integer.toHexString(address));
		} else {
			throw new RuntimeException("NOT " + Integer.toHexString(address));
		}
	}

	int AF() {
        return A << 8 | F;
    }
	
	int AF2() {
        return A2 << 8 | F2;
    }

    void AF(int AF) {
        this.A = AF >> 8;
        this.F = AF & 0xFF;
    }
    
    void AF2(int AF) {
        this.A2 = AF >> 8;
        this.F2 = AF & 0xFF;
    }
	
	void BC(int data) {
		B = data >> 8;
		C = data & 0xFF;
	}
	
	void BC2(int data) {
		B2 = data >> 8;
		C2 = data & 0xFF;
	}
	
	void DE(int data) {
		D = data >> 8;
		E = data & 0xFF;
	}
	
	void DE2(int data) {
		D2 = data >> 8;
		E2 = data & 0xFF;
	}
	
	void HL(int data) {
		H = data >> 8;
		L = data & 0xFF;
	}
	
	void HL2(int data) {
		H2 = data >> 8;
		L2 = data & 0xFF;
	}
	
	int BC() {
		return (B << 8) | C;
	}
	
	int DE() {
		return (D << 8) | E;
	}
	
	int HL() {
		return (H << 8) | L;
	}
	
	int BC2() {
		return (B2 << 8) | C2;
	}
	
	int DE2() {
		return (D2 << 8) | E2;
	}
	
	int HL2() {
		return (H2 << 8) | L2;
	}

	boolean bitTest(int address, int position) {
        return ((address & (1 << position)) != 0);
    }

    int bitSet(int address, int position) {
        return address | (1 << position);
    }

    int bitReset(int address, int position) {
        return address & ~(1 << position);
    }
	
    public final String hex(int reg) {
        String s = Integer.toHexString(reg).toUpperCase();
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }

    public final String hex4(int reg) {
        String s = Integer.toHexString(reg).toUpperCase();
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }
    
    void setSignFlag() {
		F = F | (1 << 7);
	}
	void clearSignFlag() {
		F = F & ~(1 << 7);
	}
	boolean isSignFlagSet() {
		return ((F & (1 << 7))) >> 7 == 1;
	}
	
	void setZeroFlag() {
		F = F | (1 << 6);
	}
	void clearZeroFlag() {
		F = F & ~(1 << 6);
	}
	boolean isZeroFlagSet() {
		return ((F & (1 << 6))) >> 6 == 1;
	}
	
	void setHalfCarryFlag() {
		F = F | (1 << 4);
	}
	void clearHalfCarryFlag() {
		F = F & ~(1 << 4);
	}
	boolean isHalfCarryFlagSet() {
		return ((F & (1 << 4))) >> 4 == 1;
	}
	
	void setParityFlag() {
		F = F | (1 << 2);
	}
	void clearParityFlag() {
		F = F & ~(1 << 2);
	}
	boolean isParityFlagSet() {
		return ((F & (1 << 2))) >> 2 == 1;
	}
	void setOverflowFlag() {
		F = F | (1 << 2);
	}
	void clearOverflowFlag() {
		F = F & ~(1 << 2);
	}
	boolean isOverflowFlagSet() {
		return ((F & (1 << 2))) >> 2 == 1;
	}
	
	void setNegativeFlag() {
		F = F | (1 << 1);
	}
	void clearNegativeFlag() {
		F = F & ~(1 << 1);
	}
	boolean isNegativeFlagSet() {
		return ((F & (1 << 1))) >> 1 == 1;
	}
	
	void setCarryFlag() {
		F = F | (1 << 0);
	}
	void clearCarryFlag() {
		F = F & ~(1 << 0);
	}
	boolean isCarryFlagSet() {
		return ((F & (1 << 0))) >> 0 == 1;
	}
	
	void calculateParity(int reg) {
		int parity = Integer.bitCount(reg);
		if ((parity % 2) == 0) {
			setParityFlag();
		} else {
			clearParityFlag();
		}
	}

    void printStack() {
    	for (int i = 0; i < 8; i++) {
			System.out.println(hex(readMemory(SP + i)));
		}
    }

	public void requestBus() {
		busRequested = true;
	}

	public void devolverBus() {
		busRequested = false;
	}
	
	boolean isBusRequested() {
		return busRequested;
	}

	public void reset() {
		reset = true;
	}

	public void writeByte(int addr, long data) {
		writeMemory(addr, (int) data);
	}
	
	public void writeWord(int addr, long data) {
		writeMemory(addr, (int) (data >> 8));
		writeMemory(addr + 1, (int) (data & 0xFF));
	}
}
