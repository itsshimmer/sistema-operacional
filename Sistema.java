// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// João Brentano, João Victor Granzinoli, Eduardo Soares e William de Lima
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
		interruptNone, interruptInvalidInstruction, interruptInvalidAddress, interruptOverflow, interruptInvalidPaging, interruptScheduler, interruptStop, interruptIO;
	}

	public enum Opcode {
		DATA, ___, // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
		JMP, JMPI, JMPIG, JMPIL, JMPIE, JMPIM, JMPIGM, JMPILM, JMPIEM, STOP, // desvios e parada
		ADDI, SUBI, ADD, SUB, MULT, // matematicos
		LDI, LDD, STD, LDX, STX, SWAP, // movimentacao
		TRAP; // instrução para interrupção de software
	}

	public final int pageSize = 16; // you may configure the pageSize here

	public class CPU extends Thread {
		// característica do processador: contexto da CPU ...
		private int pc; // ... composto de program counter,
		private Word ir; // instruction register,
		private int[] reg; // registradores da CPU

		final private int schedulerLimit = 5;
		private int schedulerClock = 0;

		private int[] pageTable;

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

		public void setContext(int _pc, int minimumMemory, int maximumMemory, int[] pageTable) { // no futuro esta funcao vai ter que ser
			pc = _pc; // limite e pc (deve ser zero nesta versao)
			interrupt = Interrupts.interruptNone;
			this.minimumMemory = minimumMemory;
			this.maximumMemory = maximumMemory;
			this.pageTable = pageTable;
		}

		private boolean valid(int address) {
			if (address < minimumMemory || address > maximumMemory) {
				interrupt = Interrupts.interruptInvalidAddress;
				return false;
			}
			return true;
		}

		public int translateLogicAddress(int address) {
			int destinationPage = address/pageSize;
			int destinationOffset = address%pageSize;
			int physicalMemoryAddress;
			try {
				physicalMemoryAddress = pageTable[destinationPage]*pageSize+destinationOffset;
				            		  //(           FRAME        )*pageSize  + offset
			} catch(IndexOutOfBoundsException e) {
				interrupt = Interrupts.interruptInvalidPaging;
				return -1;
			}
			return physicalMemoryAddress;
		}

		public void run() { // execucao da CPU supoe que o contexto da CPU, vide acima, esta devidamente
							// setado
			while (true) { // ciclo de instrucoes. acaba cfe instrucao, veja cada caso.
				// FETCH
				if (valid(translateLogicAddress(pc))) {
					ir = m[translateLogicAddress(pc)]; // busca posicao da memoria apontada por pc, guarda em ir
					// EXECUTA INSTRUCAO NO ir
					switch (ir.opc) { // para cada opcode, sua execução

						case LDI: // Rd ← k
							reg[ir.r1] = ir.p;
							pc++;
							break;

						case STD: // [A] ← Rs
							if (!valid(translateLogicAddress(ir.p))) { 
								interrupt = Interrupts.interruptInvalidAddress;
								break; 
								// infelizmente nao podemos deixar executar o caso para tratar a interrupcao depois apenas, pois o 
								// java ira dar excecao e ira parar
							}
							m[translateLogicAddress(ir.p)].opc = Opcode.DATA;
							m[translateLogicAddress(ir.p)].p = reg[ir.r1];
							pc++;
							break;

						case ADD: // Rd ← Rd + Rs
							try {
								reg[ir.r1] = Math.addExact(reg[ir.r1], reg[ir.r2]);
							} catch(ArithmeticException e){
								interrupt = Interrupts.interruptOverflow;
							}	
							pc++;
							break;

						case ADDI: // Rd ← Rd + k
							try {
								reg[ir.r1] = Math.addExact(reg[ir.r1], ir.p);
							} catch(ArithmeticException e){
								interrupt = Interrupts.interruptOverflow;
							}			
							pc++;
							break;

						case STX: // [Rd] ←Rs
							if (!valid(translateLogicAddress(reg[ir.r1]))) {
								interrupt = Interrupts.interruptInvalidAddress;
							}
							m[translateLogicAddress(reg[ir.r1])].opc = Opcode.DATA;
							m[translateLogicAddress(reg[ir.r1])].p = reg[ir.r2];
							pc++;
							break;

						case SUB: // Rd ← Rd - Rs
							try { 
								reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
							} catch(StackOverflowError e){
								interrupt = Interrupts.interruptOverflow;
							}			
							pc++;
							break;

						case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
							if (reg[ir.r2] > 0) {
								pc = reg[ir.r1];
							} else {
								pc++;
							}
							break;

						case STOP: 
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
					schedulerClock++;
					if(schedulerClock >= schedulerLimit) {
						interrupt = Interrupts.interruptScheduler;
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
		public CPU cpu1;
		public MemoryManager memoryManager;
		public ProcessManager processManager;

		public VM(InterruptHandler interruptHandler, TrapHandler trapHandler) { // vm deve ser configurada com endereço
																				// de tratamento de interrupcoes
			// memória
			tamMem = 1024;
			m = new Word[tamMem]; // m ee a memoria
			for (int i = 0; i < tamMem; i++) {
				m[i] = new Word(Opcode.___, -1, -1, -1);
			};

			// cpu
			cpu1 = new CPU(m, interruptHandler, trapHandler);

			cpu1.maximumMemory = tamMem - 1;
			cpu1.minimumMemory = 0;

			memoryManager = new MemoryManager(m);
			processManager = new ProcessManager(cpu1, memoryManager);
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

	public enum State {
		RUNNING,
		READY,
		BLOCKED,
		FINISHED
	}

	public class ProcessControlBlock {
		int id;
		int[] memoryPages;
		int pc;
		int[] reg;
		State state;

		public ProcessControlBlock(int id, int[] memoryPages) {
			this.id = id;
			this.memoryPages = memoryPages;
			pc = 0;
			reg = new int[10];
			state = State.READY;
		}
	}
	
	public class ProcessManager {
		private ProcessControlBlock runningProcess;
		private int currentProcessIdentifier = 0;
		private CPU cpu;
		private MemoryManager memoryManager;
		private ArrayList<ProcessControlBlock> processList = new ArrayList<>();

		public ProcessManager(CPU cpu, MemoryManager memoryManager) {
			this.cpu = cpu;
			this.memoryManager = memoryManager;
		}

		// createProcess function
		// success: true
		// failed: false
		public boolean createProcess(Word[] program) {
			int[] memoryPages = memoryManager.alloc(program); // tries to allocate the program in the memory
			if(memoryPages == null) {
				System.out.println("ProcessManager.createProcess: couldn't allocate program");	
				// System.out.println("ProcessManager.createProcess: couldn't allocate program");
				// System.out.println("ProcessManager.createProcess: couldn't allocate program");	
				return false; // couldn't allocate program
			}
			ProcessControlBlock newProcess = new ProcessControlBlock(currentProcessIdentifier, memoryPages);
			processList.add(newProcess);
			currentProcessIdentifier++;
			return true;
		}

		//run processs by order
		public boolean run() {
			try {
				runningProcess = processList.get(0);
				processList.remove(runningProcess);
			} catch(IndexOutOfBoundsException e){
				// no processes to run
				return false;
			}
			cpu.setContext(0, cpu.minimumMemory, cpu.maximumMemory, runningProcess.memoryPages);
			cpu.run();
			return true;
		}
		
		//run specific process
		public boolean runProcess(int id) {
			ProcessControlBlock currentProcess;
			try {
				currentProcess = processList.get(id);
			} catch(IndexOutOfBoundsException e){
				// invalid process id
				return false;
			}
			cpu.setContext(0, cpu.minimumMemory, cpu.maximumMemory, currentProcess.memoryPages);
			cpu.run();
			return true;
		}

		// killProcess function
		// success: true
		// failed: false
		public boolean killProcess(int id) {
			ProcessControlBlock currentProcess;
			try {
				currentProcess = processList.get(id);
			} catch(IndexOutOfBoundsException e){
				// invalid process id
				return false;
			}
			memoryManager.free(currentProcess.memoryPages);
			processList.remove(currentProcess);
			return true;
		}

		public void scheduler() {
			runningProcess.reg = cpu.reg;
			runningProcess.pc = cpu.pc;

			if(runningProcess.state != State.FINISHED) {
				processList.add(runningProcess);
			}

			if(!processList.isEmpty()) {

				if(processList.get(0).state != State.BLOCKED) {
					runningProcess = processList.get(0);
					processList.remove(runningProcess);
	
					cpu.pc = runningProcess.pc;
					cpu.reg = runningProcess.reg;
					cpu.pageTable = runningProcess.memoryPages;
					cpu.interrupt = Interrupts.interruptNone;
	
					//---------------------------------------------------SCHEDULER TEST
					// Aux aux = new Aux();
					// aux.dump(vm.m, 0, 128);
					// try {
					// 	Thread.sleep(1000);
					// } catch (InterruptedException e) {
					// 	e.printStackTrace();
					// }
					//---------------------------------------------------END SCHEDULER TEST
					cpu.run();
				} else {
					processList.add(runningProcess);
				}
				
			}
		}

		public void stop() {
			runningProcess.state = State.FINISHED;
			scheduler();
		}


	}

	public class MemoryManager {
		private Word[] memory;
		int availableFrames;
		int totalFrames;
		int allocatedFrames = 0;
		private Boolean[] memoryMap;

		public MemoryManager(Word[] memory) {
			this.memory = memory;
			availableFrames = memory.length/pageSize;
			totalFrames = availableFrames;
			memoryMap = new Boolean[availableFrames];
			for(int i = 0; i<availableFrames; i++) {
				memoryMap[i] = false;
			}
		}

		private int findAvailableFrame() {

			// iterates through all frames
			for(int i = 0; i < totalFrames; i++) {

				// if an available frame is found returns
				if(memoryMap[i] == false) {
					return i;
				}
			}
			// error
			return -1;
		}

		// alloc function
		// success: int[] with frames
		// failed: null
		public int[] alloc(Word[] words) {
			if (words.length<1) { // invalid alloc request
				System.out.println("MemoryManager.alloc: invalid alloc request");
				return null;
			} 
			if (availableFrames==0) {  // no available memory
				System.out.println("MemoryManager.alloc: no available memory");
				return null;
			}

			// calculates how many frames we need
			int neededFrames = (int)Math.ceil((double)words.length/(double)pageSize);

			// check if we have enough frames
			if (neededFrames<=availableFrames) {
				
				// array to store filled frames index
				int[] frames = new int[neededFrames];

				// index to iterate data received
				int lastInsertedIndex = 0;

				// fills X frames(as needed)
				for(int currentNewFrame = 0; currentNewFrame<neededFrames; currentNewFrame++) {

					// finds frame to fill
					int frame = findAvailableFrame(); 

					// fills appropriate memory position with data
					for(int frameOffset = 0; frameOffset<pageSize; frameOffset++) {
						if(lastInsertedIndex+frameOffset>=words.length) {
							break;
						}
						memory[frame*pageSize+frameOffset].opc = words[lastInsertedIndex+frameOffset].opc;
						memory[frame*pageSize+frameOffset].r1 = words[lastInsertedIndex+frameOffset].r1;
						memory[frame*pageSize+frameOffset].r2 = words[lastInsertedIndex+frameOffset].r2;
						memory[frame*pageSize+frameOffset].p = words[lastInsertedIndex+frameOffset].p;
					}
					lastInsertedIndex = lastInsertedIndex + pageSize; // controls the iteration of the words(data)
					memoryMap[frame] = true; // frame is now occupied
					frames[currentNewFrame] = frame; // stores frame id
					availableFrames--;
					allocatedFrames++;
				}
				// success
				return frames;				
			}
			// fails because there's not enough frames available
			System.out.println("MemoryManager.alloc: fails because there's not enough frames available");
			return null;
		}

		public void free(int[] frames) {
			
			// iterates through frames that need freeing
			for(int index = 0; index < frames.length; index++) { 

				// if memory is allocated, frees it
				if(memoryMap[frames[index]] = true) {
					memoryMap[frames[index]] = false;
					availableFrames++;
					allocatedFrames--;
				}
			}
		}
	
	}

	public class InterruptHandler {

		public void handle(Interrupts interrupt) {
			
			switch(interrupt) {
				case interruptScheduler:
					System.out.println("_interruptScheduler_");
					vm.cpu1.schedulerClock = 0;
					vm.processManager.scheduler();
					break;

				case interruptStop:
					System.out.println("***interruptStop***");
					vm.cpu1.schedulerClock = 0;
					vm.processManager.stop();
					break;

				case interruptIO:

					break; 

				default:
					System.out.println("A interruption has just happened! " + interrupt);
					break;
			}
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
		// PROGRAMA NORMAL FIBONACCI

		// Sistema s = new Sistema();
		// s.test1();

		// PROGRAMA INTERRUPÇÃO ENDEREÇO INVÁLIDO

		// Sistema s = new Sistema();
		// s.errorMemory();

		// PROGRAMA TESTE CHAMADA DE SISTEMA INPUT

		// Sistema s = new Sistema();
		// s.programTestTrapInput();

		// PROGRAMA TESTE CHAMADA DE SISTEMA OUTPUT

		// Sistema s = new Sistema();
		// s.programTestTrapOutput();

		// PROGRAMA INTERRUPÇÃO INSTRUÇÃO INVÁLIDA

		// Sistema s = new Sistema();
		// s.programTestInvalidInstruction();

		// PROGRAMA TESTE OVERFLOW

		// Sistema s = new Sistema();
		// s.programTestOverflow();

		// PROGRAMA TESTE MEMORY MANAGER

		// Sistema s = new Sistema();
		// s.programTestMemManager();

		// PROGRAM TEST RUN ALL
		
		// Sistema s = new Sistema();
		// s.programTestRunAll();

		// PROGRAM TEST SCHEDULER
		
		// Sistema s = new Sistema();
		// s.programTestScheduler();

		//////////////////////////////////////////// FASE 6

		Sistema sistema = new Sistema();
		sistema.fase6();
	}
	// -------------------------------------------------------------------------------------------------------
	// --------------- TUDO ABAIXO DE MAIN É AUXILIAR PARA FUNCIONAMENTO DO SISTEMA
	// - nao faz parte

	// -------------------------------------------- teste do sistema , veja classe
	// de programas

	public void test1() {
		Aux aux = new Aux();
		Word[] program = new Programas().fibonacci10;
		
		System.out.println("---------------------------------- programa carregado ");
		vm.processManager.createProcess(program);
		aux.dump(vm.m, 0, 32);
		System.out.println("---------------------------------- após execucao ");
		vm.processManager.run();
		aux.dump(vm.m, 0, 32);
	}

	public void errorMemory() {
		Aux aux = new Aux();
		Word[] program = new Programas().programab;

		System.out.println("---------------------------------- programa carregado ");
		vm.processManager.createProcess(program);
		aux.dump(vm.m, 0, 32);
		System.out.println("---------------------------------- após execucao ");
		vm.processManager.run();
		aux.dump(vm.m, 0, 32);
	}

	public void programTestTrapInput() {
		Aux aux = new Aux();
		Word[] program = new Programas().programTestTrapInput;
		
		System.out.println("---------------------------------- programa carregado ");
		vm.processManager.createProcess(program);
		aux.dump(vm.m, 0, 10);
		System.out.println("---------------------------------- após execucao ");
		vm.processManager.run();
		aux.dump(vm.m, 0, 10);
	}

	public void programTestTrapOutput() {
		Aux aux = new Aux();
		Word[] program = new Programas().programTestTrapOutput;
		
		System.out.println("---------------------------------- programa carregado ");
		vm.processManager.createProcess(program);
		aux.dump(vm.m, 0, 10);
		System.out.println("---------------------------------- após execucao ");
		vm.processManager.run();
		aux.dump(vm.m, 0, 10);
	}

	public void programTestInvalidInstruction() {
		Aux aux = new Aux();
		Word[] program = new Programas().programTestInvalidInstruction;
		
		System.out.println("---------------------------------- programa carregado ");
		vm.processManager.createProcess(program);
		aux.dump(vm.m, 0, 10);
		System.out.println("---------------------------------- após execucao ");
		vm.processManager.run();
		aux.dump(vm.m, 0, 10);
	}

	public void programTestOverflow() {
		Aux aux = new Aux();
		Word[] program = new Programas().programTestOverflow;
		
		System.out.println("---------------------------------- programa carregado ");
		vm.processManager.createProcess(program);
		aux.dump(vm.m, 0, 10);
		System.out.println("---------------------------------- após execucao ");
		vm.processManager.run();
		aux.dump(vm.m, 0, 10);
	}

	public void programTestMemManager() {
		Aux aux = new Aux();
		Word[] program;
		boolean status;

		program = new Programas().data1;
		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		program = new Programas().data2;
		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		aux.dump(vm.m, 0, 97);

		// keep in mind that memory is only MARKED as free, its not overwritten with empty data
		vm.processManager.killProcess(1);

		program = new Programas().data3;
		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		program = new Programas().data2;
		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		aux.dump(vm.m, 0, 97);
	}

	public void programTestRunAll() {
		Aux aux = new Aux();

		Word[] program;
		boolean status;

		program = new Programas().fibonacci10;
		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		program = new Programas().data1;
		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		vm.processManager.killProcess(1);

		program = new Programas().fibonacci10;
		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		vm.processManager.killProcess(1);		

		aux.dump(vm.m, 0, 128);

		vm.processManager.run();
		System.out.println("----------------------------- after run all");
		aux.dump(vm.m, 0, 128);
	}
	
	public void programTestScheduler() {
		// In order to test the scheduler, please uncomment the lines in ProcessManager.schedule()
		Aux aux = new Aux();

		Word[] program;
		boolean status;

		program = new Programas().fibonacci10;
		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		status = vm.processManager.createProcess(program);
		System.out.println("new process successful? "+ status);

		vm.processManager.killProcess(1);

		aux.dump(vm.m, 0, 128);

		vm.processManager.run();

		System.out.println("----------------------------- after run all");
		aux.dump(vm.m, 0, 128);
	}

	public void fase6() {
		while(true) {
			System.out.println("menu");
		}
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
			new Word(Opcode.STOP, -1, -1, -1), // multiple stops represt memory needed to run program
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
			new Word(Opcode.STOP, -1, -1, -1),
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

			// interrupção endereço inválido
			new Word(Opcode.LDI, 1, -1, 59),
			new Word(Opcode.STD, 1, -1, 1024),

			new Word(Opcode.STOP, 1, -1, 0) };

		// programa para teste de chamada de sistema, leitura
		public Word[] programTestTrapInput = new Word[] { 
			new Word(Opcode.LDI, 8, -1, 1), 
			new Word(Opcode.LDI, 9, -1, 8),
			new Word(Opcode.TRAP, -1, -1, -1),

			new Word(Opcode.STOP, 1, -1, 0)};

		public Word[] programTestTrapOutput = new Word[] { 
			new Word(Opcode.LDI, 8, -1, 2), // registrador definido para output(trap)
			new Word(Opcode.LDI, 9, -1, 8), // registrador define output para conteudo do endereço 59
			new Word(Opcode.STD, 9, -1, 8), // armazena no endereço 59 o conteudo do r9(8)

			new Word(Opcode.TRAP, -1, -1, -1), // chamada de sistema, saida deve ser DATA 8 do endereço 8

			new Word(Opcode.STOP, 1, -1, 0)};

		// programa para teste de chamada de sistema, leitura
		public Word[] programTestInvalidInstruction = new Word[] { 
			new Word(Opcode.___, 8, -1, 1), 
			new Word(Opcode.LDI, 9, -1, 50),
			new Word(Opcode.TRAP, -1, -1, -1),

			new Word(Opcode.STOP, 1, -1, 0),

			new Word(Opcode.DATA, 50, -1, 1) };

		public Word[] programTestOverflow = new Word[] { 
			new Word(Opcode.LDI, 0, -1, 2147483647), 
			new Word(Opcode.LDI, 1, -1, 1236), 
			new Word(Opcode.ADD, 0, 1, -1),

			new Word(Opcode.STD, 0, -1, 8), 
			new Word(Opcode.STD, 1, -1, 9), 

			new Word(Opcode.STOP, 1, -1, 0),

			new Word(Opcode.DATA, 50, -1, 1) };

		public Word[] data1 = new Word[] { // data1 for testing
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1),
			new Word(Opcode.DATA, 1, -1, 1)};

		public Word[] data2 = new Word[] { // data2 for testing
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1),
			new Word(Opcode.DATA, 2, -1, 1)};

		public Word[] data3 = new Word[] { // data3 for testing
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1),
			new Word(Opcode.DATA, 3, -1, 1)};


	}

	

}
