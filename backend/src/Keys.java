enum Keys {
    A, D, W, S, M, R, SPACE, ENTER, P, E, L, K;

    int mask() {
        return 1 << ordinal();
    }
}
