import sric::*;

struct A {
    var a: own* Int;
    var b: ref* Int;
    var c: raw* Int;
    var d: WeakPtr$<Int>;
}

fun foo(c: ref* Int) {
}

fun foo2(c: own* Int) {
}

fun main()
{
    var i: refable Int = 1;
    var p: ref* Int = &i;
    foo(p);

    var p2: own* Int = alloc$<Int>();
    foo(p2);

    p = p2;

    var x: own*? Int;
    foo(x!);

    foo2(move p2);

    if (x != null) {
        printf("%d", *x);
    }
}
