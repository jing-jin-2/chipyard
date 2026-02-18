// See LICENSE.Sifive for license details.
#include <stdint.h>

#include <platform.h>

#include "common.h"

#define DEBUG
#include "kprintf.h"

#define EASY_PRG_SEQ 	"EASYPROG"

#define CMD_LEN			8

#define S_WAIT_CMD		0
#define S_WAIT_LEN		1
#define S_RECV_PRG		2

#define EASY_TRUE		(1==1)
#define EASY_FALSE		(1!=1)

#define EASY_BUFLEN		64

#define DEBUG_BUILD

#ifdef DEBUG_BUILD
	#define DBG(fmt, ...)  	kprintf(fmt, __VA_ARGS__)
#else
	#define DBG(fmt, ...)	do {} while(0)
#endif

int circular_check(char* buf, int begin, int buf_len, char* seq);

int main(void) {
	volatile uint8_t* exec_ptr = (volatile uint8_t*) 0x80000000;

	char easy_prg_seq[] = EASY_PRG_SEQ;
	char cmd_buf[CMD_LEN] = {0};

	REG32(uart, UART_REG_TXCTRL) = UART_TXEN;
	REG32(uart, UART_REG_RXCTRL) = UART_RXEN;

	int cmd_idx = 0;
	int state = S_WAIT_CMD;
	int jump_state = S_WAIT_CMD;
	uint32_t txn_len = 0;
	uint32_t txn_ctr = 0;

	uint8_t prog_buf[EASY_BUFLEN];

	DBG("%s\n", "Started Frontend.");

	while (EASY_TRUE) {
		switch (state) {
		case S_WAIT_CMD:
			cmd_buf[cmd_idx] = kgetc();
			cmd_idx = (cmd_idx + 1) % CMD_LEN;
			if (circular_check(cmd_buf, cmd_idx, CMD_LEN, easy_prg_seq)) {
				jump_state = S_RECV_PRG;
				state = S_WAIT_LEN;
				txn_len = 0;
				txn_ctr = 0;
			}
			break;
		case S_WAIT_LEN: 
			txn_len = (txn_len << 8) | kgetc();
			txn_ctr++;
			if (txn_ctr == 4) {
				state = jump_state;
				txn_ctr = 0;
			}
			break;
		case S_RECV_PRG:
			kputc(EASY_BUFLEN);
			for (int i = 0; i < EASY_BUFLEN && i + txn_ctr < txn_len; i++) {
				prog_buf[i] = kgetc();
			}
			for (int i = 0; i < EASY_BUFLEN && txn_ctr < txn_len; i++) {
				exec_ptr[txn_ctr++] = prog_buf[i];
			}
			if (txn_ctr >= txn_len) {
				state = S_WAIT_CMD;
				DBG("COMMAND: %s\n", "DONE READING");
				kputs("JUMPING TO PROGRAM\n");
				__asm__ __volatile__ ("fence.i" ::: "memory");
				return 0;
				/* Old way to boot, should not be necessary with new versions of chipyard */
				// int (*workload)(void) = (int (*)(void))exec_ptr;
				// workload();
			}
			break;
		}
	}
	return 0;
}

int circular_check(char* buf, int begin, int buf_len, char* seq) {
	for (int i = 0; i < buf_len && seq[i]; i++) {
		if (buf[(begin + i) % buf_len] != seq[i]) {
			return EASY_FALSE;
		}
	}
	return EASY_TRUE;
}
