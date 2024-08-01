import { extend } from "umi-request";

const request = extend({
    prefix: process.env.NODE_ENV === 'production' ? 'http://wodiaonima.aabb.com' : undefined
})