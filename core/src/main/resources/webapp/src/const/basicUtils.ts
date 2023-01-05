export const getRandomInt = (min: number, max: number) => {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min + 1)) + min;
};

export const sortObjectByKeys = (obj: Record<string, any>, recursive: boolean = false): Record<string, any> => {
    return Object.keys(obj)
        .sort()
        .reduce((map: Record<string, any>, key: string) => {
            // check for null
            if (obj[key] && typeof obj[key] == 'object' && recursive) {
                map[key] = sortObjectByKeys(obj[key], recursive);
            } else {
                map[key] = obj[key];
            }
            return map;
        }, {});
};
