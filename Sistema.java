// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// Fase 1 - máquina virtual (vide enunciado correspondente)
//

import java.util.*;

public class Sistema {

	// -------------------------------------------------------------------------------------------------------
	// --------------------- H A R D W A R E - definicoes de HW
	// ----------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// --------------------- M E M O R I A - definicoes de opcode e palavra de
	// memoria ----------------------

	public class Word { // cada posicao da memoria tem uma instrucao (ou um dado)
		public Opcode opc; //
		public int r1; // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
		public int r2; // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
		public int p; // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

		public Word(Opcode _opc, int _r1, int _r2, int _p) {
			opc = _opc;
			r1 = _r1;
			r2 = _r2;
			p = _p;
		}
	}
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// --------------------- C P U - definicoes da CPU
	// -----------------------------------------------------

	public enum Interrupts {
		interruptNone, interruptInvalidInstruction, interruptInvalidAddress, interruptStop;
	}

	public enum Opcode {
		DATA, ___, // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
		JMP, JMPI, JMPIG, JMPIL, JMPIE, JMPIM, JMPIGM, JMPILM, JMPIEM, STOP, // desvios e parada
		ADDI, SUBI, ADD, SUB, MULT, // matematicos
		LDI, LDD, STD, LDX, STX, SWAP, // movimentacao
		TRAP; // instrução para interrupção de software
	}

	public class CPU {
		// característica do processador: contexto da CPU ...
		private int pc; // ... composto de program counter,
		private Word ir; // instruction register,
		private int[] reg; // registradores da CPU

		private Interrupts interrupt; // instancia as interrupcoes
		private InterruptHandler interruptHandler;
		private TrapHandler trapHandler;

		private int minimumMemory; // armazenar as bordas da memoria
		private int maximumMemory;

		private Word[] m; // CPU acessa MEMORIA, guarda referencia 'm' a ela. memoria nao muda. ee sempre
							// a mesma.

		public CPU(Word[] _m, InterruptHandler interruptHandler, TrapHandler trapHandler) { // ref a MEMORIA e interrupt
																							// handler passada na
																							// criacao da CPU
			m = _m; // usa o atributo 'm' para acessar a memoria.
			reg = new int[10]; // aloca o espaço dos registradores
			this.interruptHandler = interruptHandler;
			this.trapHandler = trapHandler;
		}

		public void setContext(int _pc, int minimumMemory, int maximumMemory) { // no futuro esta funcao vai ter que ser
			pc = _pc; // limite e pc (deve ser zero nesta versao)
			interrupt = Interrupts.interruptNone;
			this.minimumMemory = minimumMemory;
			this.maximumMemory = maximumMemory;
		}

		private boolean valid(int address) {
			if (address < minimumMemory || address > maximumMemory) {
				interrupt = Interrupts.interruptInvalidAddress;
				return false;
			}
			return true;
		}

		public void run() { // execucao da CPU supoe que o contexto da CPU, vide acima, esta devidamente
							// setado
			while (true) { // ciclo de instrucoes. acaba cfe instrucao, veja cada caso.
				// FETCH
				if (valid(pc)) {
					ir = m[pc]; // busca posicao da memoria apontada por pc, guarda em ir
					// EXECUTA INSTRUCAO NO ir
					switch (ir.opc) { // para cada opcode, sua execução

					case LDI: // Rd ← k
						reg[ir.r1] = ir.p;
						pc++;
						break;

					case STD: // [A] ← Rs
						if (!valid(ir.p)) { 
							interrupt = Interrupts.interruptInvalidAddress;
							break;
						}
						m[ir.p].opc = Opcode.DATA;
						m[ir.p].p = reg[ir.r1];
						pc++;
						break;

					case ADD: // Rd ← Rd + Rs
						reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
						pc++;
						break;

					case ADDI: // Rd ← Rd + k
						reg[ir.r1] = reg[ir.r1] + ir.p;
						pc++;
						break;

					case STX: // [Rd] ←Rs
						if (!valid(reg[ir.r1])) {
							interrupt = Interrupts.interruptInvalidAddress;
						}
						m[reg[ir.r1]].opc = Opcode.DATA;
						m[reg[ir.r1]].p = reg[ir.r2];
						pc++;
						break;

					case SUB: // Rd ← Rd - Rs
						reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
						pc++;
						break;

					case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
						if (reg[ir.r2] > 0) {
							pc = reg[ir.r1];
						} else {
							pc++;
						}
						break;

					case STOP: // por enquanto, para execucao
						interrupt = Interrupts.interruptStop;
						break;

					case TRAP:
						trapHandler.trap(this);
						pc++;
						break;

					default:
						interrupt = Interrupts.interruptInvalidInstruction;
						break;

					}
				}
				// VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
				if (!(interrupt == Interrupts.interruptNone)) {
					interruptHandler.handle(interrupt);
					break; // break sai do loop da cpu
				}
			}
		}
	}
	// ------------------ C P U - fim
	// ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// ------------------- V M - constituida de CPU e MEMORIA
	// -----------------------------------------------
	// -------------------------- atributos e construcao da VM
	// -----------------------------------------------
	public class VM {
		public int tamMem;
		public Word[] m;
		public CPU cpu;

		public VM(InterruptHandler interruptHandler, TrapHandler trapHandler) { // vm deve ser configurada com endereço
																				// de tratamento de interrupcoes
			// memória
			tamMem = 1024;
			m = new Word[tamMem]; // m ee a memoria
			for (int i = 0; i < tamMem; i++) {
				m[i] = new Word(Opcode.___, -1, -1, -1);
			}
			;
			// cpu
			cpu = new CPU(m, interruptHandler, trapHandler);
		}
	}
	// ------------------- V M - fim
	// ------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// --------------------H A R D W A R E - fim
	// -------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// ------------------- S O F T W A R E - inicio
	// ----------------------------------------------------------

	public class InterruptHandler {

		public void handle(Interrupts interrupts) {
			System.out.println("A interruption has just happened! " + interrupts);
		}

	}

	public class TrapHandler {
		Aux aux = new Aux();

		public void trap(CPU cpu) {
			System.out.println("A system call has just happened! " + cpu.reg[8] + "|" + cpu.reg[9]);

			switch (cpu.reg[8]) { // register 8 stores what needs to be done in the system call
			case 1: // in this case we'll store data in the address stored in register 9
				System.out.println("Please input an integer:");
				Scanner input = new Scanner(System.in);
				int anInt = input.nextInt();
				cpu.m[cpu.reg[9]].p = anInt; // stores the input
				cpu.m[cpu.reg[9]].opc = Opcode.DATA; // sets the destination as DATA
				System.out.println("Value stored in the position " + cpu.reg[9]);
				System.out.println("Stored value: ");
				aux.dump(cpu.m[cpu.reg[9]]); // dumps the memory of the vm at the address that was set in register 9
				break;

			case 2: // in this case we'll print the data in the address stored in register 9
				System.out.println("Output: ");
				aux.dump(cpu.m[cpu.reg[9]]);
				break;
			}
		}

	}

	// ------------------- VAZIO

	// -------------------------------------------------------------------------------------------------------
	// ------------------- S I S T E M A
	// --------------------------------------------------------------------
	public InterruptHandler interruptHandler;
	public TrapHandler trapHandler;
	public VM vm;

	public Sistema() { // a VM com tratamento de interrupções

		interruptHandler = new InterruptHandler();
		trapHandler = new TrapHandler();
		vm = new VM(interruptHandler, trapHandler);
	}

	// ------------------- S I S T E M A - fim
	// --------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// ------------------- instancia e testa sistema
	public static void main(String args[]) {
		// PROGRAMA NORMAL

		Sistema s = new Sistema();
		s.test1();

		// PROGRAMA ERRO MEMORIA

		// Sistema s = new Sistema();
		// s.errorMemory();

		// PROGRAMA TESTE CHAMADA DE SISTEMA

		// Sistema s = new Sistema();
		// s.programTestTrap();
	}
	// -------------------------------------------------------------------------------------------------------
	// --------------- TUDO ABAIXO DE MAIN É AUXILIAR PARA FUNCIONAMENTO DO SISTEMA
	// - nao faz parte

	// -------------------------------------------- teste do sistema , veja classe
	// de programas

	public void test1() {
		Aux aux = new Aux();
		Word[] p = new Programas().fibonacci10;
		aux.carga(p, vm.m);
		vm.cpu.setContext(0, 0, vm.tamMem - 1);
		System.out.println("---------------------------------- programa carregado ");
		aux.dump(vm.m, 0, 33);
		System.out.println("---------------------------------- após execucao ");
		vm.cpu.run();
		aux.dump(vm.m, 0, 33);
	}

	// public void test2(){
	// Aux aux = new Aux();
	// Word[] p = new Programas().progMinimo;
	// aux.carga(p, vm.m);
	// vm.cpu.setContext(0, 0, vm.tamMem - 1);
	// System.out.println("---------------------------------- programa carregado ");
	// aux.dump(vm.m, 0, 15);
	// System.out.println("---------------------------------- após execucao ");
	// vm.cpu.run();
	// aux.dump(vm.m, 0, 15);
	// }

	public void errorMemory() {
		Aux aux = new Aux();
		Word[] p = new Programas().programab;
		aux.carga(p, vm.m);
		// vm.cpu.setContext(0, vm.tamMem - 1, 0);
		vm.cpu.setContext(0, 0, vm.tamMem - 1);
		System.out.println("---------------------------------- programa carregado ");
		aux.dump(vm.m, 0, vm.tamMem);
		System.out.println("---------------------------------- após execucao ");
		vm.cpu.run();
		aux.dump(vm.m, 0, vm.tamMem);
	}

	public void programTestTrap() {
		Aux aux = new Aux();
		Word[] p = new Programas().programTestTrap;
		aux.carga(p, vm.m);
		vm.cpu.setContext(0, 0, vm.tamMem - 1);
		System.out.println("---------------------------------- programa carregado ");
		aux.dump(vm.m, 0, 70);
		System.out.println("---------------------------------- após execucao ");
		vm.cpu.run();
		aux.dump(vm.m, 0, 70);
	}

	// ------------------------------------------- classes e funcoes auxiliares
	public class Aux {
		public void dump(Word w) {
			System.out.print("[ ");
			System.out.print(w.opc);
			System.out.print(", ");
			System.out.print(w.r1);
			System.out.print(", ");
			System.out.print(w.r2);
			System.out.print(", ");
			System.out.print(w.p);
			System.out.println("  ] ");
		}

		public void dump(Word[] m, int ini, int fim) {
			for (int i = ini; i < fim; i++) {
				System.out.print(i);
				System.out.print(":  ");
				dump(m[i]);
			}
		}

		public void carga(Word[] p, Word[] m) {
			for (int i = 0; i < p.length; i++) {
				m[i].opc = p[i].opc;
				m[i].r1 = p[i].r1;
				m[i].r2 = p[i].r2;
				m[i].p = p[i].p;
			}
		}
	}
	// ------------------------------------------- fim classes e funcoes auxiliares

	// -------------------------------------------- programas aa disposicao para
	// copiar na memoria (vide aux.carga)
	public class Programas {
		public Word[] progMinimo = new Word[] { new Word(Opcode.LDI, 0, -1, 999), new Word(Opcode.STD, 0, -1, 10),
			new Word(Opcode.STD, 0, -1, 11),
			new Word(Opcode.STD, 0, -1, 12), 
			new Word(Opcode.STD, 0, -1, 13),
			new Word(Opcode.STD, 0, -1, 14), 
			new Word(Opcode.STOP, -1, -1, -1) };

		// programa operando de forma regular, fibonacci
		public Word[] fibonacci10 = new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
			new Word(Opcode.LDI, 1, -1, 0), 
			new Word(Opcode.STD, 1, -1, 20), // 50
			new Word(Opcode.LDI, 2, -1, 1), 
			new Word(Opcode.STD, 2, -1, 21), // 51
			new Word(Opcode.LDI, 0, -1, 22), // 52
			new Word(Opcode.LDI, 6, -1, 6), 
			new Word(Opcode.LDI, 7, -1, 31), // 61
			new Word(Opcode.LDI, 3, -1, 0), 
			new Word(Opcode.ADD, 3, 1, -1), 
			new Word(Opcode.LDI, 1, -1, 0),
			new Word(Opcode.ADD, 1, 2, -1), 
			new Word(Opcode.ADD, 2, 3, -1), 
			new Word(Opcode.STX, 0, 2, -1),
			new Word(Opcode.ADDI, 0, -1, 1), 
			new Word(Opcode.SUB, 7, 0, -1), 
			new Word(Opcode.JMPIG, 6, 7, -1),
			new Word(Opcode.STOP, -1, -1, -1) };

		// tentativa de implementação de um exercício, posteriormente usado apenas para
		// demonstração de endereço inválido ao final
		public Word[] programab = new Word[] { 
			new Word(Opcode.LDI, 1, -1, 50), 
			new Word(Opcode.LDI, 7, -1, 7),
			new Word(Opcode.JMPIG, 7, 1, -1), 
			new Word(Opcode.LDI, 7, -1, 69), 
			new Word(Opcode.STD, 7, -1, 60),
			new Word(Opcode.STOP, 1, -1, 0), 
			new Word(Opcode.LDI, 2, -1, 0), 
			new Word(Opcode.ADD, 2, 1, -1),
			new Word(Opcode.LDI, 6, -1, 1), 
			new Word(Opcode.SUB, 1, 6, -1), 
			new Word(Opcode.LDI, 7, -1, 8),
			new Word(Opcode.JMPIG, 7, 1, -1),

			// grava todos os registradores para fins de debug
			new Word(Opcode.STD, 0, -1, 50), 
			new Word(Opcode.STD, 1, -1, 51), 
			new Word(Opcode.STD, 2, -1, 52),
			new Word(Opcode.STD, 3, -1, 53), 
			new Word(Opcode.STD, 4, -1, 54), 
			new Word(Opcode.STD, 5, -1, 55),
			new Word(Opcode.STD, 6, -1, 56), 
			new Word(Opcode.STD, 7, -1, 57), 

			new Word(Opcode.LDI, 1, -1, 59),
			new Word(Opcode.STD, 1, -1, 1024),

			new Word(Opcode.STOP, 1, -1, 0) };

		// programa para teste de chamada de sistema, leitura
		public Word[] programTestTrap = new Word[] { 
			new Word(Opcode.LDI, 8, -1, 1), 
			new Word(Opcode.LDI, 9, -1, 50),
			new Word(Opcode.TRAP, -1, -1, -1),

			new Word(Opcode.STOP, 1, -1, 0),

			new Word(Opcode.DATA, 50, -1, 1) };

	}

}
