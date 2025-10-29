#include <stdio.h>

int plus(int a, int b) {
    return a + b;
}
int minus(int a, int b) {
    return a - b;
}
int time(int a, int b) {
    return a * b;
}
int divided(int a, int b) {
    return a / b;
}
int main() {
    char c;
    int a, b;
    while (1) {
        printf("Enter 2 integers>> ");
        scanf_s("%d%d", &a, &b);
        if (a == 0 || b == 0) break;

        printf("Enter operator>> ");
        scanf_s(" %c", &c);

        if (c == '+') {
            printf("%d\n", plus(a, b));//Ochirsuren//
        }
        else if (c == '-') {
            printf("%d\n", minus(a, b));
        }
        else if (c == '*') {
            printf("%d\n", time(a, b));
        }
        else if (c == '/') {
            printf("%d\n", divided(a, b));
        }
        printf("\n");
    }
    return 0;
}
