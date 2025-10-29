#include <stdio.h>

int main() {
    char c;
    int a, b;
    while (1) {
        printf("Enter 2 integers>> ");
        scanf_s("%d%d", &a, &b);
        printf("Enter operator>> ");
        scanf_s(" %c", &c);

        if (c == '+') {
            printf("%d\n", plus(a, b));
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
    }
    return 0;
}
