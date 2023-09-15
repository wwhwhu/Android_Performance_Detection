#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
//#include <unistd.h>

int main()
{
	printf("Hello Android!\n");
	// 查看当前内存
	long mem_avail0 = -1;//可用的内存
	char name0[20];
	FILE *fp0;
	char buf10[128], buf20[128], buf30[128], buf40[128], buf50[128];
	int buff_len = 128;
	fp0 = fopen("/proc/meminfo", "r");
	if (fp0 == NULL) {
		printf("can't open");
        return 0;
	}
	if (NULL == fgets(buf10, buff_len, fp0) ||
	        NULL == fgets(buf20, buff_len, fp0) ||
	        NULL == fgets(buf30, buff_len, fp0) ||
	        NULL == fgets(buf40, buff_len, fp0) ||
	        NULL == fgets(buf50, buff_len, fp0)) {
		printf("can't read");
		fclose(fp0);
		return 0;
	}
	fclose(fp0);
	sscanf(buf30, "%s%ld", name0, &mem_avail0);
    printf("当前Mem_Avail: %ldMB\n",mem_avail0/1024);
	//固定任务分配LMK留一个G
	int num1= (mem_avail0/1024 - 216)/100 - 10;
	int *p1=0;
	int *pArr1[130];
	int cnt1=0;
	//开辟内存，1024*1024个字节，一次分配100M
	for(int i = 0; i<num1; i++) {
		p1 = malloc(1024 * 256 * sizeof(int) * 100);
		if(p1!=NULL) {
			pArr1[cnt1] = p1 ;
			// 查看被分配的地址空间
			printf("第 %d 次固定分配,分配地址为：%p \n",cnt1,p1);
			// 为分配的内存赋值为rand()
			memset(p1,rand(),sizeof(int)*256*1024*100);
			// 查看赋值结果
			printf("*p1 = %d \n",*p1);
			cnt1++;
		}
	}
	// 打印结果
	printf("为固定分配任务分配了 %d00MB的空间\n",cnt1);
	sleep(5);

	//动态任务分配num*50M
	int num=30;
	int *p=0;
	// 记录分配的地址
	int *pArr[30];
	// 记录指针是否被分配，初始为0，未分配
	int fArr[30]= {0};
	int cnt=0;
	// 当p始终能够分配出空间，就进行计数
	for(int i = 0; i<num; i++) {
		//每次分配100M
		p=(int *)malloc(sizeof(int) * 256 * 1024 * 50);
		if(p!=NULL) {
			pArr[cnt] = p ;
			fArr[cnt] = 1 ;
			// 查看被分配的地址空间
			printf("动态分配第 %d 块分配地址为：%p \n",cnt,p);
			// 为分配的内存赋值为0
			memset(p,rand(),sizeof(int)*256*1024*50);
			// 查看赋值结果
			printf("*p = %d \n",*p);
			cnt++;
		}
	}

	// 打印结果
	printf("为动态分配任务1分配了 %d00MB的空间\n",cnt);

	// 持续执行
	while(1==1) {
		// 睡眠5s后查看能否再次分配
		sleep(5);
		printf("checking...\n");

		// 查看当前内存
		long mem_avail = -1;//可用的内存，=总内存-使用了的内存
		char name[20];

		FILE *fp;
		char buf1[128], buf2[128], buf3[128], buf4[128], buf5[128];
		fp = fopen("/proc/meminfo", "r");
		if (fp == NULL) {
			printf("can't open");
			continue;
		}
		if (NULL == fgets(buf1, buff_len, fp) ||
		        NULL == fgets(buf2, buff_len, fp) ||
		        NULL == fgets(buf3, buff_len, fp) ||
		        NULL == fgets(buf4, buff_len, fp) ||
		        NULL == fgets(buf5, buff_len, fp)) {
			printf("can't read");
			fclose(fp);
			continue;
		}
		fclose(fp);
		sscanf(buf3, "%s%ld", name, &mem_avail);
		printf("memavail: %ldMB\n",mem_avail/1024);


		// 剩余空间不足50MB时，释放一部分
		if(mem_avail<216*1024) {
			printf("剩余空间不足,为 %ld MB\n",mem_avail/1024);
			int freeCount = 0;
			for(int i = 0; i<num; i++) {
				if(fArr[i]==1) {
					free(pArr[i]);
					fArr[i]=0;
					freeCount++;
				}
				if(freeCount>2) {
					printf("释放了100MB");
					break;
				}
			}
		}
		// 剩余空间大于100MB时，重新分配
		else if(mem_avail>(100+216)*1024) {
			printf("剩余空间充足,为%ldMB \n",mem_avail/1024);
			for(int i = 0; i<num; i++) {
				if(fArr[i]==0) {
					//单次分配50M/
					p=(int *)malloc(sizeof(int) * 256 * 1024 * 50);
					if(p!=NULL) {
						pArr[i] = p ;
						fArr[cnt] = 1 ;
						// 查看被分配的地址空间
						printf("追加分配地址为动态地址第 %d 块：%p \n",i,p);
						// 为分配的内存赋值为0
						memset(p,rand(),sizeof(int)*256*1024*50);
					}
				}
			}

		}

		// 为已分配的内存重新赋值
		for(int i =0; i<num; i++) {
			if (fArr[i]==1) {
				// 查看被分配的地址空间
				printf("目前已经分配地址为动态地址第%d块：%p \n", i , pArr[i]);
				// 为分配的内存赋值为0
				memset(pArr[i],rand(),sizeof(int)*256*1024*50);
			}
		}
	}
	// 释放空间;
	free(p);
	return 0;
}